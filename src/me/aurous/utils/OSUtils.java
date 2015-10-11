package me.aurous.utils;

public class OSUtils {
	public enum OS {
		WINDOWS, LINUX, OSX, UNKNOWN,
	}

	public static OS getOS() {
		final String osName = System.getProperty("os.name").toLowerCase();

		if (osName.indexOf("win") >= 0) {
			return OS.WINDOWS;
		} else if ((osName.indexOf("nix") >= 0) || (osName.indexOf("nux") >= 0)
				|| (osName.indexOf("aix") > 0)) {
			return OS.LINUX;
		} else if (osName.indexOf("mac") >= 0) {
			return OS.OSX;
		} else {
			return OS.UNKNOWN;
		}
	}
}