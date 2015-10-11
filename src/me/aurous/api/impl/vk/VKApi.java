package me.aurous.api.impl.vk;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.aurous.utils.Internet;

public class VKApi {

	public static final String VK_API_PREFIX = "https://api.vk.com/method/";
	protected int applicationId, userId;
	protected String accessToken;

	public VKApi(final int applicationId, final String formData) {
		this.applicationId = applicationId;
		final Pattern accessTokenPattern = Pattern
				.compile("access_token\\=([^\\&]*)\\&");
		final Matcher accessTokenMatcher = accessTokenPattern.matcher(formData);
		if (!accessTokenMatcher.find()) {
			throw new RuntimeException(
					"Unexpected form data. Something goes wrong?!");
		}
		this.accessToken = accessTokenMatcher.group(1);
		this.userId = Integer.parseInt(formData.substring(formData
				.indexOf("user_id=") + "user_id=".length()).trim());
	}

	public VKApi(final VKApi api) {
		this.applicationId = api.applicationId;
		this.userId = api.userId;
		this.accessToken = new String(api.accessToken);
	}

	public String generateQuery(final String methodName, final String parameters) {
		return VK_API_PREFIX + methodName + "?" + parameters + "&access_token="
				+ this.accessToken;
	}

	public String submitQuery(final String methodName, final String parameters)
			throws IOException {
		return Internet.text(generateQuery(methodName, parameters));
	}

	@Override
	public String toString() {
		return "applicationId: " + this.applicationId + "; userId: "
				+ this.userId + "; accessToken: " + this.accessToken;
	}
}
