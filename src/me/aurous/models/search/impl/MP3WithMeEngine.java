package me.aurous.models.search.impl;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import me.aurous.config.ServiceConstants;
import me.aurous.exceptions.ExceptionWidget;
import me.aurous.models.search.SearchEngine;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.teamdev.jxbrowser.chromium.Browser;

public class MP3WithMeEngine extends SearchEngine {
	private final Browser browser;
	private final String phrase;

	public MP3WithMeEngine(final Browser browser, final String phrase) {
		this.browser = browser;
		this.phrase = phrase;

	}

	/**
	 * Used to build an mp3 skull search query. An initial request might be
	 * needed to fetch the CSRF token from the page source before searching
	 *
	 * @param query
	 * @param csrf
	 * @return
	 * @throws URISyntaxException
	 */
	@SuppressWarnings("deprecation")
	public URI getBaseSearchURI(final String query) throws URISyntaxException {
		final List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair("q", query));
		return URIUtils.createURI(ServiceConstants.Mp3WithMe.API_SCHEME,
				ServiceConstants.Mp3WithMe.API_HOST, -1,
				ServiceConstants.Mp3WithMe.API_SEARCH,
				URLEncodedUtils.format(qparams, "UTF-8"), null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean search() {

		try {
			final JSONObject results = new JSONObject();
			final JSONArray resultsArray = new JSONArray();
			final String uri = getBaseSearchURI(phrase).toString();
			Document doc;
			try {

				final InputStream input = new URL(uri).openStream();
				doc = Jsoup.parse(input, "ISO-8859-1", uri);

			} catch (final Exception e) {
				final String script = "handleException('" + phrase + "');";
				browser.executeJavaScript(script);
				return true;
			}

			final Elements ul = doc.select("ul[class=songs]");
			final Elements li = ul.select("li");
			results.put("phrase", phrase);
			for (int i = 0; i < li.size(); i++) {
				final JSONObject resultsObject = new JSONObject();

				final String title = li.get(i).select("div[class=song]")
						.select("strong").first().text();

				resultsObject.put("title", title);
				final String artist = li.get(i).select("strong[class=artist]")
						.text();
				resultsObject.put("artist", artist);
				resultsObject.put("album", "Unknown");
				resultsObject.put("duration", "Unknown");

				final Elements link = li.get(i).select("a[href]");
				final String absHref = link.attr("abs:href"); // "http://jsoup.org/"
				resultsObject.put("link", absHref);
				resultsArray.add(resultsObject);

			}

			results.put("results", resultsArray);

			final String bytesEncoded = Base64.encode(results.toJSONString()
					.getBytes());

			String script = "searchCallback('%s');";
			script = String.format(script, bytesEncoded);
			script = script.replaceAll("[\r\n]+", " ");
			// System.out.println(results.toJSONString());
			browser.executeJavaScript(script);

			return true;
		} catch (final Exception e) {
		//	ExceptionWidget widget = new ExceptionWidget(e);
		//	widget.showWidget();
		}
		return false;
	}
}
