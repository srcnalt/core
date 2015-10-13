package me.aurous.utils;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import javax.imageio.ImageIO;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Created by Kenneth on 9/4/2014.
 */
public class Internet {

	public static Connection connect(final String url) {
		return Jsoup.connect(url);
	}

	public static Document document(final String url) {
		final Connection conn = connect(url);
		try {
			return conn.get();
		} catch (final IOException e) {

		}
		return null;
	}

	public static void openUrl(String link) {
		try {
			URLConnection connection = new URL(link).openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
			connection.connect();

			BufferedReader r  = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));

			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
			    sb.append(line);
			}
		} catch (Exception ex) {
		}
	}
	public static Image image(final String url) {

		try {
			return ImageIO.read(new URL(url));
		} catch (final IOException e) {

		}
		return null;
	}

	public static String text(final String url) {

		final StringBuilder builder = new StringBuilder();
		URLConnection conn = null;
		try {
			conn = new URL(url).openConnection();
		} catch (final IOException e) {

		}
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
		} catch (final IOException e) {

		}
		String input;
		try {
			while ((input = reader.readLine()) != null) {
				builder.append(input);
			}
		} catch (final IOException e) {

		}
		return builder.toString();
	}

	public static void postFile(final String playListPath) {

	}

}
