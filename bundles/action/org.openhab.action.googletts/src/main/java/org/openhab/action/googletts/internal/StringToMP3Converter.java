package org.openhab.action.googletts.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;

public class StringToMP3Converter {
	/**
	 * URL to query for Google synthesiser
	 */
	private final static String GOOGLE_SYNTHESISER_URL = "http://translate.google.com/translate_tts?tl=";
	/**
	 * language of the Text you want to translate
	 */
	private LanguageCode languageCode;

	public enum LanguageCode {
		LANG_AU_ENGLISH("en-AU"), LANG_US_ENGLISH("en-US"), LANG_UK_ENGLISH(
				"en-GB"), LANG_ES_SPANISH("es"), LANG_FR_FRENCH("fr"), LANG_DE_GERMAN(
				"de"), LANG_PT_PORTUGUESE("pt-pt"), LANG_PT_BRAZILIAN("pt-br");

		private final String languageCodeString;

		private LanguageCode(String languageCodeString) {
			this.languageCodeString = languageCodeString;

		}

		public String getLanguageCodeString() {
			return languageCodeString;
		}

		public static LanguageCode fromString(String languageCodeStr) {
			for (LanguageCode languageCode : values()) {
				if (StringUtils.equals(languageCode.getLanguageCodeString(),
						languageCodeStr)) {
					return languageCode;
				}
			}

			return null;
		}
	}

	/**
	 * Constructor that takes language code parameter. Specify to "auto" for
	 * language autoDetection
	 * 
	 * @throws Exception
	 */
	public StringToMP3Converter(final LanguageCode languageCode) {
		if (languageCode == null) {
			this.languageCode = LanguageCode.LANG_US_ENGLISH;

		} else {
			this.languageCode = languageCode;
		}
	}

	/**
	 * Gets an input stream to MP3 data for the returned information from a
	 * request
	 * 
	 * @param synthText
	 *            Text you want to be synthesized into MP3 data
	 * @return Returns an input stream of the MP3 data that is returned from
	 *         Google
	 * @throws IOException
	 *             Throws exception if it can not complete the request
	 */
	public InputStream getMP3Data(String synthText) throws IOException {
		if (synthText.length() > 100) {
			List<String> fragments = parseString(synthText);
			InputStream out = getMP3Data(fragments);
			return out;
		}
		String encoded = URLEncoder.encode(synthText, "UTF-8");
		URL url = new URL(GOOGLE_SYNTHESISER_URL
				+ languageCode.getLanguageCodeString() + "&q=" + encoded);

		URLConnection urlConn = url.openConnection();
		urlConn.addRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:2.0) Gecko/20100101 Firefox/4.0");
		return urlConn.getInputStream();
	}

	/**
	 * Gets an InputStream to MP3Data for the returned information from a
	 * request
	 * 
	 * @param synthText
	 *            List of Strings you want to be synthesized into MP3 data
	 * @return Returns an input stream of all the MP3 data that is returned from
	 *         Google
	 * @throws IOException
	 *             Throws exception if it cannot complete the request
	 */
	public InputStream getMP3Data(List<String> synthText) throws IOException {
		// Uses an executor service pool for concurrency. Limit to 1000 threads
		// max.
		ExecutorService pool = Executors.newFixedThreadPool(1000);
		// Stores the Future (Data that will be returned in the future)
		Set<Future<InputStream>> set = new LinkedHashSet<Future<InputStream>>(
				synthText.size());
		for (String part : synthText) { // Iterates through the list
			Callable<InputStream> callable = new MP3DataFetcher(part);// Creates
																		// Callable
			Future<InputStream> future = pool.submit(callable);// Begins to run
																// Callable
			set.add(future);// Adds the response that will be returned to a set.
		}
		List<InputStream> inputStreams = new ArrayList<InputStream>(set.size());
		for (Future<InputStream> future : set) {
			try {
				inputStreams.add(future.get());// Gets the returned data from
												// the future.
			} catch (ExecutionException e) {// Thrown if the MP3DataFetcher
											// encountered an error.
				Throwable ex = e.getCause();
				if (ex instanceof IOException) {
					throw (IOException) ex;// Downcasts and rethrows it.
				}
			} catch (InterruptedException e) {// Will probably never be called,
												// but just in case...
				Thread.currentThread().interrupt();// Interrupts the thread
													// since something went
													// wrong.
			}
		}
		return new SequenceInputStream(Collections.enumeration(inputStreams));// Sequences
																				// the
																				// stream.
	}

	/**
	 * Separates a string into smaller parts so that Google will not reject the
	 * request.
	 * 
	 * @param input
	 *            The string you want to separate
	 * @return A List<String> of the String fragments from your input..
	 */
	private List<String> parseString(String input) {
		return parseString(input, new ArrayList<String>());
	}

	/**
	 * Separates a string into smaller parts so that Google will not reject the
	 * request.
	 * 
	 * @param input
	 *            The string you want to break up into smaller parts
	 * @param fragments
	 *            List<String> that you want to add stuff too. If you don't have
	 *            a List<String> already constructed "new ArrayList<String>()"
	 *            works well.
	 * @return A list of the fragments of the original String
	 */
	private List<String> parseString(String input, List<String> fragments) {
		if (input.length() <= 100) {// Base Case
			fragments.add(input);
			return fragments;
		} else {
			int lastWord = findLastWord(input);// Checks if a space exists
			if (lastWord <= 0) {
				fragments.add(input.substring(0, 100));// In case you sent
														// gibberish to Google.
				return parseString(input.substring(100), fragments);
			} else {
				fragments.add(input.substring(0, lastWord));// Otherwise, adds
															// the last word to
															// the list for
															// recursion.
				return parseString(input.substring(lastWord), fragments);
			}
		}
	}

	/**
	 * Finds the last word in your String (before the index of 99) by searching
	 * for spaces and ending punctuation. Will preferably parse on punctuation
	 * to alleviate mid-sentence pausing
	 * 
	 * @param input
	 *            The String you want to search through.
	 * @return The index of where the last word of the string ends before the
	 *         index of 99.
	 */
	private int findLastWord(String input) {
		if (input.length() < 100)
			return input.length();
		int space = -1;
		for (int i = 99; i > 0; i--) {
			char tmp = input.charAt(i);
			if (isEndingPunctuation(tmp)) {
				return i + 1;
			}
			if (space == -1 && tmp == ' ') {
				space = i;
			}
		}
		if (space > 0) {
			return space;
		}
		return -1;
	}

	/**
	 * Checks if char is an ending character Ending punctuation for all
	 * languages according to Wikipedia (Except for Sanskrit non-unicode)
	 * 
	 * @param The
	 *            char you want check
	 * @return True if it is, false if not.
	 */
	private boolean isEndingPunctuation(char input) {
		return input == '.' || input == '!' || input == '?' || input == ';'
				|| input == ':' || input == '|';
	}

	/**
	 * This class is a callable. A callable is like a runnable except that it
	 * can return data and throw exceptions. Useful when using futures.
	 * Dramatically improves the speed of execution.
	 * 
	 * @author Aaron Gokaslan (Skylion)
	 */
	private class MP3DataFetcher implements Callable<InputStream> {
		private final String synthText;

		public MP3DataFetcher(String synthText) {
			this.synthText = synthText;
		}

		@Override
		public InputStream call() throws IOException {
			return getMP3Data(synthText);
		}
	}
}
