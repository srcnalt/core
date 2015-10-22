package me.aurous.models.search.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import me.aurous.models.search.SearchEngine;
import me.aurous.parallel.Parallel;
import me.aurous.utils.AurousStringUtils;
import me.aurous.utils.Utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.teamdev.jxbrowser.chromium.Browser;

import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;

public class SoundCloudEngine extends SearchEngine {
	private final Browser browser;
	private final String phrase;
	Integer limit = 50;
	final SoundCloud soundcloud = new SoundCloud(
			"14df3cafac4d329bda1dfd62b07564c5",
			"96feda4264c9d20bcdf98329ab15baf3");

	public SoundCloudEngine(final Browser browser, final String phrase) {
		this.browser = browser;
		this.phrase = phrase;
	}

	public String getStreamableUrl(final String trackId) {
		String url = "https://api.soundcloud.com/tracks/%s/stream?client_id=14df3cafac4d329bda1dfd62b07564c5";
		url = String.format(url, trackId);
		try {
			final URLConnection con = new URL(url).openConnection();
			con.connect();
			final InputStream is = con.getInputStream();
			is.close();
			return con.getURL().toString();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return trackId;

	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean search() {
		final JSONObject results = new JSONObject();
		final JSONArray resultsArray = new JSONArray();

		soundcloud.put("limit", limit);
		final ArrayList<Track> result = soundcloud.findTrack(phrase);

		{
			Parallel.awaitFor(result,
					param -> {
						if ((param != null) && (param.isStreamable() != null)
								&& param.isStreamable()) {
							final JSONObject resultsObject = new JSONObject();
							
							final String title = AurousStringUtils.UTFEncode(param.getTitle());
							resultsObject.put("title", title);
							
							final String artist = AurousStringUtils.UTFEncode(param.getUser().getUsername());
							resultsObject.put("artist", artist);
							
							final String duration = Utils.formatSeconds((int) ( param.getDuration() / 1000.0));
							resultsObject.put("duration", duration);
			
							
							String url = getStreamableUrl(param.getId()
									.toString());
							
							resultsObject.put("link", url);
							resultsArray.add(resultsObject);
						}
					});
			results.put("results", resultsArray);
			final String bytesEncoded = Base64.encode(results.toJSONString()
					.getBytes());

			String script = "searchCallback('%s');";
			script = String.format(script, bytesEncoded);
			script = script.replaceAll("[\r\n]+", " ");
			browser.executeJavaScript(script);
			return true;

		}

	}

}
