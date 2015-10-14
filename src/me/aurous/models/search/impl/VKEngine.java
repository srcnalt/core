package me.aurous.models.search.impl;

import java.io.IOException;
import java.nio.charset.Charset;

import me.aurous.api.auth.impl.VKAuth;
import me.aurous.api.impl.vk.audio.AudioApi;
import me.aurous.config.AppConstants;
import me.aurous.exceptions.ExceptionWidget;
import me.aurous.models.search.SearchEngine;
import me.aurous.utils.AurousStringUtils;
import me.aurous.utils.Utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.teamdev.jxbrowser.chromium.Browser;

public class VKEngine extends SearchEngine {
	private final Browser browser;
	private final String phrase;
	private final int RESULT_LIMIT = 50;
	
	public VKEngine(final Browser browser, final String phrase) {
		this.browser = browser;
		this.phrase = phrase;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean search() {

			final JSONObject results = new JSONObject();
			final JSONArray resultsArray = new JSONArray();
			final AudioApi api = new AudioApi(VKAuth.VK_APP_ID, Utils.readFile(AppConstants.dataPath() + "vkauth.dat", Charset.forName("UTF-8")));
			final String parameters = String
					.format("q=%s&auto_complete=1&sort=2&lyrics=0&count=%s&performer_only=%s",
							AurousStringUtils.UTFEncode(phrase), this.RESULT_LIMIT, 0);
			
			try {
				final String searchJSON = api.searchAudioJson(parameters);
				if (searchJSON.contains("\"response\":[0]")) {
					final String script = "handleException('" + phrase + "');";
					browser.executeJavaScript(script);
					return true;
				}
				
				final JSONObject jsonObj = (JSONObject) JSONValue.parse(searchJSON); 
				final JSONArray response = (JSONArray) jsonObj.get("response");
				for (int i = 1, size = response.size(); i < size; i++) {
					final JSONObject resultsObject = new JSONObject();
					final Object jsonObject = response.get(i);
					final JSONObject jsonResults = (JSONObject) JSONValue.parse(jsonObject.toString()); 
					String artist =AurousStringUtils.UTFEncode( jsonResults.get("artist").toString());
					String title = AurousStringUtils.UTFEncode(jsonResults.get("title").toString());
					final String stream = jsonResults.get("url").toString();
					final String duration =  Utils.formatSeconds(Integer
							.parseInt(jsonResults.get("duration").toString()));
					resultsObject.put("title", title);
					resultsObject.put("artist", artist);
					resultsObject.put("duration", duration);
					resultsObject.put("album", "Unknown");
					resultsObject.put("link", stream);
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
			} catch (IOException e) {
			//	ExceptionWidget widget = new ExceptionWidget(e);
				//widget.showWidget();
			}
			
		return false;
	}
}
