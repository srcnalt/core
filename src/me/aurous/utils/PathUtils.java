package me.aurous.utils;

public class PathUtils {
	public static String combine(final String path1, final String path2) {
		if (path1.endsWith("/")
				|| path1.endsWith(System.getProperty("file.separator"))) {
			return path1 + path2;
		} else {
			return path1 + System.getProperty("file.separator") + path2
					+ System.getProperty("file.separator");
		}
	}

	public static String combine(final String path1, final String path2,
			final String path3) {
		return combine(combine(path1, path2), path3);
	}

	public static String combine(final String path1, final String path2,
			final String path3, final String path4) {
		return combine(combine(path1, path2), combine(path3, path4));
	}
}