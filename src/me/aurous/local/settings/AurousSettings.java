package me.aurous.local.settings;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import me.aurous.config.AppConstants;
import me.aurous.exceptions.ExceptionWidget;
import me.aurous.utils.Utils;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.teamdev.jxbrowser.chromium.Browser;

public class AurousSettings {
	public String avatarPath;
	public String displayName;
	public String defaultSearchEngine;
	public String[] scanPaths;
	public String[] scanRestrictions;
	public String[] scanExcludes;
	public boolean invertedNav;
	public String settingsPath = AppConstants.dataPath() + "settings.dat";


	// public HashMap<String, String> hashmap = new HashMap<String, String>();

	public void saveSettings(final String avatarPath, final String displayName,
			final String defaultSearchEngine, final String[] scanPaths,
			final String[] scanRestrictions, final String[] scanExcludes, final boolean invertedNav) {
		this.avatarPath = avatarPath;
		this.displayName = displayName;
		this.defaultSearchEngine = defaultSearchEngine;
		this.scanPaths = scanPaths;
		this.scanRestrictions = scanRestrictions;
		this.scanExcludes = scanExcludes;
		this.invertedNav = invertedNav;
	}

	public String[] getScanPaths() {
		try {
			final GSONHelper helper = new GSONHelper();
			final AurousSettings loaded = (AurousSettings) helper.loadClass(
					new FileInputStream(settingsPath), AurousSettings.class);
			return loaded.scanPaths;
		} catch (final IOException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
		return null;
	}

	public String getSearchEngine() {
		try {
			final GSONHelper helper = new GSONHelper();
			final AurousSettings loaded = (AurousSettings) helper.loadClass(
					new FileInputStream(settingsPath), AurousSettings.class);
			return loaded.defaultSearchEngine;
		} catch (final IOException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
		return null;
	}

	
	public void loadSettings(final Browser browser) {

		final String data = Utils.readFile(settingsPath,
				StandardCharsets.UTF_8);
		final String bytesEncoded = Base64.encode(data.getBytes());

		String script = "settings.load('%s');";
		script = String.format(script, bytesEncoded);
		script = script.replaceAll("[\r\n]+", " ");

		browser.executeJavaScript(script);

	}

	public void saveDefaults() {
		final GSONHelper helper = new GSONHelper();
		this.avatarPath = "assets/img/avatar.png";
		this.displayName = "Aurous User";
		this.defaultSearchEngine = "Aurous Network";
		this.scanPaths = new String[0];
		this.scanRestrictions = new String[0];
		this.scanExcludes = new String[0];
		this.invertedNav = false;
		try {
			helper.saveClass(new FileOutputStream(
					this.settingsPath), this);
		} catch (final IOException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
	}
}