package me.aurous.api.auth.impl.youtube;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import me.aurous.config.AppConstants;
import me.aurous.utils.Internet;
import me.aurous.utils.Utils;

/**
 * @author Andrew
 *
 */
public class YouTubeDecoder {
	private String streamURL;
	private final String SITE_HTML;
	private final String contentURL;
	private final String PLAYER_VERSION_REGEX = "\\\\/\\\\/s.ytimg.com\\\\/yts\\\\/jsbin\\\\/html5player-(.+?)\\.js";
	private final String URL_ENCODE_REGEX = "\\\"url_encoded_fmt_stream_map\\\":\\s*\\\"([^\\\"]+)\\\"";
	private final String URL_STREAMS_REGEX = "(^url=|(\\\\u0026url=|,url=))(.+?)(\\\\u0026|,|$)";
	private final String STREAM_SIGNATURES_REGEX = "(^s=|(\\\\u0026s=|,s=))(.+?)(\\\\u0026|,|$)";

	public YouTubeDecoder(final String contentURL) {
		this.contentURL = contentURL;
		this.SITE_HTML = Internet.text(this.contentURL);
	}

	public String grab() {
		String lowQualityMP4 = null;
		String highQualityMP4 = null;
		String webM = null;
		try {

			final List<String> list = extractURLS(this.SITE_HTML);

			for (final String url : list) {

				if (url.contains("itag=18")) {
					lowQualityMP4 = url;
				} else if (url.contains("itag=22")) {
					highQualityMP4 = url;
				} else if (url.contains("itag=43")) {
					webM = url;
				}
			}
			// if (Settings.isStreamLowQuality() == true) {
			// this.streamURL = lowQualityMP4;
			// }
			if (Utils.isNull(highQualityMP4) && Utils.isNull(webM)) {
				this.streamURL = lowQualityMP4;
			} else if (Utils.isNull(highQualityMP4) && !Utils.isNull(webM)) {
				this.streamURL = webM;
			} else {
				this.streamURL = highQualityMP4;
			}

			return this.streamURL;
		} catch (final UnsupportedEncodingException e) {

			// final ExceptionWidget eWidget = new ExceptionWidget(e);
			// eWidget.showWidget();

			e.printStackTrace();
		}
		return this.streamURL;

	}

	public String getStreamURL() {
		return this.streamURL;
	}

	private List<String> extractURLS(final String html)
			throws UnsupportedEncodingException {

		final List<String> streams = new ArrayList<String>();
		final List<String> signatures = new ArrayList<String>();
		String playerVersion = "";
		Pattern pattern = Pattern.compile(this.PLAYER_VERSION_REGEX);
		Matcher matcher = pattern.matcher(html);
		while (matcher.find()) {
			playerVersion = matcher.group(1).toString();
		}
		if (Utils.isNull(AppConstants.HTML5_PLAYER_CODE)) { // grab once so we
			// don't have to
			// pull it down each
			// time

			AppConstants.HTML5_PLAYER_CODE = Internet
					.text("http://s.ytimg.com/yts/jsbin/" + "html5player-"
							+ playerVersion.replace("\\", "") + ".js");
			// System.out.println(AppConstants.HTML5_PLAYER_CODE );

		}

		pattern = Pattern.compile(this.URL_ENCODE_REGEX);

		matcher = pattern.matcher(html);

		String unescapedHtml = "";
		while (matcher.find()) {
			unescapedHtml = matcher.group(1);

		}

		pattern = Pattern.compile(this.URL_STREAMS_REGEX);

		matcher = pattern.matcher(unescapedHtml);

		while (matcher.find()) {

			streams.add(URLDecoder.decode(matcher.group(3), "UTF-8"));

		}

		pattern = Pattern.compile(this.STREAM_SIGNATURES_REGEX);

		matcher = pattern.matcher(unescapedHtml);

		while (matcher.find()) {

			signatures.add(URLDecoder.decode(matcher.group(3), "UTF-8"));

		}
		final List<String> urls = new ArrayList<String>();

		for (int i = 0; i < (streams.size() - 1); i++) {
			String URL = streams.get(i).toString();

			if (signatures.size() > 0) {
				final String Sign = signDecipher(signatures.get(i).toString(),
						AppConstants.HTML5_PLAYER_CODE);
				URL += "&signature=" + Sign;

			}

			urls.add(URL.trim());

		}

		return urls;

	}

	private String signDecipher(final String signature, final String playercode) {
		try {
			// System.out.println(signature);
			final ScriptEngine engine = new ScriptEngineManager()
			.getEngineByName("nashorn");
			engine.eval(new FileReader(AppConstants.dataPath()
					+ "decryptYoutube.js"));
			final Invocable invocable = (Invocable) engine;

			final Object result = invocable.invokeFunction("getWorkingVideo",
					signature, playercode);
			// System.out.println((String) result);
			return (String) result;
		} catch (ScriptException | FileNotFoundException
				| NoSuchMethodException e) {
			// final ExceptionWidget eWidget = new ExceptionWidget(e);
			// eWidget.showWidget();
			e.printStackTrace();
		}
		return "error";
	}

}