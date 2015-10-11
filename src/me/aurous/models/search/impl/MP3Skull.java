package me.aurous.models.search.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.aurous.config.ServiceConstants;
import me.aurous.exceptions.ExceptionWidget;
import me.aurous.models.search.SearchEngine;
import me.aurous.utils.Utils;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.teamdev.jxbrowser.chromium.Browser;

public class MP3Skull extends SearchEngine {
	private final Browser browser;
	private final String phrase;

	public MP3Skull(final Browser browser, final String phrase) {
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
	public URI getBaseSearchURI(final String query, final String csrf)
			throws URISyntaxException {
		final List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair("q", query));
		qparams.add(new BasicNameValuePair("fckh", csrf));
		return URIUtils.createURI(ServiceConstants.Mp3skull.API_SCHEME,
				ServiceConstants.Mp3skull.API_HOST, -1,
				ServiceConstants.Mp3skull.API_SEARCH,
				URLEncodedUtils.format(qparams, "UTF-8"), null);
	}

	@Override
	public boolean search() {
		final String baseUrl = ServiceConstants.Mp3skull.API_SCHEME + "://"
				+ ServiceConstants.Mp3skull.API_HOST;
		try {
			final String userAgent = "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1667.0 Safari/537.36";
			final Document doc = Jsoup.connect(baseUrl).userAgent(userAgent)
					.followRedirects(false).get();
			final Element csrfElement = doc.select("input[name=fckh]").first();
			if (csrfElement == null) {
				throw new Exception("Cannot find CSRF element on the page");
			}
			final String uri = getBaseSearchURI(phrase,
					csrfElement.attr("value")).toString();
			final Document searchDoc = Jsoup.connect(uri).userAgent(userAgent)
					.followRedirects(false).get();

			final Elements songs = searchDoc
					.getElementsByAttributeValueMatching("id", "song_html");
			for (final Element songElement : songs) {
				String meta = "";
				String title = "";
				String url = "";
				try {
					meta = songElement.getElementsByClass("left").first()
							.text();
				} catch (final Exception e) {
					meta = "Error";
				}

				if (!meta.equals("Error")) {
					final Matcher m = Pattern.compile("\\s([0-9]+)\\s*kbps")
							.matcher(meta);

					while (m.find()) {
					}
				}
				try {
					title = Utils
							.deDup(songElement.getElementById("right_song")
									.getElementsByTag("div").first().text())
									.replace("mp3", "").trim();
				} catch (final Exception e) {
					title = "Error";
				}

				try {
					url = songElement.getElementsByClass("download_button")
							.first().select("a").attr("href");
				} catch (final Exception e) {
					url = "Error";
				}

				final String total = meta + "," + title + "," + url;

				System.out.println(total);

				System.out.println("");
				System.out.println("");
				browser.executeJavaScript("console.log('Still searching');");
			}
		} catch (final Exception e) {
	//		ExceptionWidget widget = new ExceptionWidget(e);
			//widget.showWidget();
		}
		return false;
	}

}
