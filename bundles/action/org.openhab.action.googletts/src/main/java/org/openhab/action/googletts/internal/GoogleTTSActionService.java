/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.googletts.internal;

import java.util.Dictionary;

import org.apache.commons.lang.StringUtils;
import org.openhab.action.googletts.internal.StringToMP3Converter.LanguageCode;
import org.openhab.core.scriptengine.action.ActionService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class registers an OSGi service for the GoogleTTS action.
 * 
 * @author cc
 * @since 1.6.0
 */
public class GoogleTTSActionService implements ActionService, ManagedService {

	private static final Logger logger = LoggerFactory
			.getLogger(GoogleTTSActionService.class);

	/**
	 * Indicates whether this action is properly configured which means all
	 * necessary configurations are set. This flag can be checked by the action
	 * methods before executing code.
	 */
	/* default */static boolean isProperlyConfigured = false;

	public GoogleTTSActionService() {
	}

	public void activate() {
	}

	public void deactivate() {
		// deallocate Resources here that are no longer needed and
		// should be reset when activating this binding again
	}

	@Override
	public String getActionClassName() {
		return GoogleTTS.class.getCanonicalName();
	}

	@Override
	public Class<?> getActionClass() {
		return GoogleTTS.class;
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	public void updated(Dictionary<String, ?> config)
			throws ConfigurationException {
		if (config != null) {

			// set language code (default: LANG_US_ENGLISH)
			LanguageCode languageCode = LanguageCode.LANG_US_ENGLISH;
			String languagecodeStr = (String) config.get("languagecode");

			if (StringUtils.isNotBlank(languagecodeStr)) {
				LanguageCode configuredLanguageCode = LanguageCode
						.fromString(languagecodeStr);

				if (languageCode != null) {
					languageCode = configuredLanguageCode;
				} else {
					logger.error("Languagecode not valid.");
				}

			}

			// create instance of StringToMP3Converter to use for TTS
			StringToMP3Converter stringToMP3Converter = new StringToMP3Converter(
					languageCode);
			GoogleTTS.stringToMP3Converter = stringToMP3Converter;

			isProperlyConfigured = true;
		}
	}

}
