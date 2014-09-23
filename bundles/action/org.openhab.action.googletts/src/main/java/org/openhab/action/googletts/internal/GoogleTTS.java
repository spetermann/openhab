/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.googletts.internal;

import java.io.IOException;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import org.openhab.core.scriptengine.action.ActionDoc;
import org.openhab.core.scriptengine.action.ParamDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the methods that are made available in scripts and rules
 * for GoogleTTS.
 * 
 * @author cc
 * @since 1.6.0
 */
public class GoogleTTS {

	private static final Logger logger = LoggerFactory
			.getLogger(GoogleTTS.class);

	static StringToMP3Converter stringToMP3Converter = null;

	// provide public static methods here

	// Example
	@ActionDoc(text = "A cool method that does some GoogleTTS", returns = "<code>true</code>, if successful and <code>false</code> otherwise.")
	public static boolean speak(
			@ParamDoc(name = "text", text = "Text") String text) {

		if (!GoogleTTSActionService.isProperlyConfigured) {
			logger.debug("GoogleTTS action is not yet configured - execution aborted!");

			return false;
		}

		try {
			Player player;
			player = new Player(stringToMP3Converter.getMP3Data(text));
			player.play();

			return true;

		} catch (JavaLayerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

}
