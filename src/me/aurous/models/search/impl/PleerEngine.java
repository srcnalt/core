package me.aurous.models.search.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import me.aurous.config.ServiceConstants;
import me.aurous.exceptions.ExceptionWidget;
import me.aurous.helpers.Internet;
import me.aurous.models.search.SearchEngine;
import me.aurous.parallel.Parallel;
import me.aurous.utils.AurousStringUtils;
import me.aurous.utils.Utils;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.teamdev.jxbrowser.chromium.Browser;

public class PleerEngine extends SearchEngine {
	private final Browser browser;
	private final String phrase;
	private final String PEER_API = "http://pleer.com/site_api/files/get_url?action=download&id=%s";

	public PleerEngine(final Browser browser, final String phrase) {
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
		qparams.add(new BasicNameValuePair("target", "tracks"));
		return URIUtils.createURI(ServiceConstants.Pleer.API_SCHEME,
				ServiceConstants.Pleer.API_HOST, -1,
				ServiceConstants.Pleer.API_SEARCH,
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

				final String html = Internet.text(uri);
				doc = Jsoup.parse(html);

			} catch (final Exception e) {
				final String script = "handleException('" + phrase + "');";
				browser.executeJavaScript(script);
				return true;
			}

			final Elements li = doc.select("li[duration]");
			final Collection<Integer> elems = new LinkedList<Integer>();
			for (int i = 0; i < li.size(); i++) {
				elems.add(i);
			}
			results.put("phrase", phrase);
			Parallel.awaitFor(
					elems,
					param -> {
						final JSONObject resultsObject = new JSONObject();

						final String title = AurousStringUtils.UTFEncode(li
								.get(param).attr("song"));
				
						resultsObject.put("title", title);
				

						final String artist = AurousStringUtils.UTFEncode(li
								.get(param).attr("singer"));
						resultsObject.put("artist", artist);

						final String duration = Utils.formatSeconds(Integer
								.parseInt(li.get(param).attr("duration")));
						resultsObject.put("duration", duration);

						final String id = li.get(param).attr("link");
						// System.out.println(id);

						final String formattedApi = String.format(PEER_API, id);

						final String pleerJson = Internet.text(formattedApi);

						final JSONParser parser = new JSONParser();
						Object obj = null;
						try {
							obj = parser.parse(pleerJson);
						} catch (final ParseException e) {
						//	ExceptionWidget widget = new ExceptionWidget(e);
							//widget.showWidget();
							return;
						}

						final JSONObject songJson = (JSONObject) obj;

						final Object url = songJson.get("track_link");

						resultsObject.put("album", "Unknown");

						resultsObject.put("link", url);
						resultsArray.add(resultsObject);
					});

			results.put("results", resultsArray);

			final String bytesEncoded = Base64.encode(results.toJSONString()
					.getBytes());

			String script = "searchCallback('%s');";
			script = String.format(script, bytesEncoded);
			script = script.replaceAll("[\r\n]+", " ");
			browser.executeJavaScript(script);
		//	System.out.println("Test");

			return true;
		} catch (final Exception e) {
//		ExceptionWidget widget = new ExceptionWidget(e);
			//widget.showWidget();
		}
		return false;
	}

}
