package me.aurous.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringEscapeUtils;

public class AurousStringUtils {

	public static String UTFEncode(final String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			return "null";
		}
	}

	/**
	 * Takes UTF-8 strings and encodes non-ASCII as
	 * ampersand-octothorpe-digits-semicolon HTML-encoded characters
	 *
	 * @param string
	 * @return HTML-encoded String
	 */
	public static String htmlEncode(final String string) {
		final StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < string.length(); i++) {
			final Character character = string.charAt(i);
			if (CharUtils.isAscii(character)) {
				// Encode common HTML equivalent characters
				stringBuffer.append(StringEscapeUtils.escapeHtml4(character
						.toString()));
			} else {
				// Why isn't this done in escapeHtml4()?
				stringBuffer.append(String.format("&#x%x;",
						Character.codePointAt(string, i)));
			}
		}
		return stringBuffer.toString();
	}
}
