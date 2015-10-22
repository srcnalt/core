package me.aurous.config;

import java.io.File;

import me.aurous.utils.OSUtils;
import me.aurous.utils.PathUtils;

public class AppConstants {
	public static String HTML5_PLAYER_CODE = null;
	public static double VERSION = 0.1;
	public static String APP_NAME = "Aurous";
	public static String INTERNAL = "Project Phoneix";
	public static boolean REMOTE_DEBUG = true;
	public static boolean VERBOSE_LOG = false;
	public static boolean BORDERLESS = true;
	public static String DEFAULT_PATH = "D:/Documents/frontend/index.html";
	public static String PRODUCTION_PATH = "./data/index.html";
	public static boolean INTERNAL_DEBUG = false;

	public static String dataPath() {

		String dir = PathUtils.combine(System.getProperty("user.home"),
				"aurous");

		switch (OSUtils.getOS()) {
		case WINDOWS:
			dir = PathUtils.combine(System.getenv("APPDATA"), "aurous");
			break;

		case LINUX:
			dir = PathUtils.combine(System.getProperty("user.home"), ".aurous");
			break;

		case OSX:
			dir = PathUtils.combine(System.getProperty("user.home"), "Library",
					"Application Support", "aurous");
			break;

		default:
			break;
		}
		final File dataPath = new File(dir);
		final boolean exists = dataPath.exists();
		if (exists) {
			return dir;
		} else {
			dataPath.mkdir();
			return dir;
		}
	}

}
