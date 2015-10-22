package me.aurous.utils;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;















import java.util.Arrays;
import java.util.LinkedHashSet;

import com.teamdev.jxbrowser.chromium.Browser;







import me.aurous.exceptions.ExceptionWidget;
import me.aurous.local.database.DatabaseManager;
import me.aurous.local.settings.AurousSettings;
import me.aurous.models.download.DownloadResult;
import me.aurous.models.download.Downloader;

public class Utils {
	private static final String[] BROWSERS = new String[] {
		"xdg-open", "google-chrome", "chromium", "firefox", "opera", "epiphany", "konqueror", "conkeror", "midori", "kazehakase", "mozilla"
};
	public static String formatSeconds(final int total) {
		final int minutes = (total / 60);
		final int seconds = (total % 60);
		String secs = Integer.toString(seconds);
		if (seconds < 10) {
			secs = "0" + seconds;
		}
		final String time = minutes + ":" + secs;
		return time;
	}

	/**
	 * Finds the first supported program in the list (for UNIX-like platforms
	 * only).
	 * @param kind The kind of program, used in the exception message if no
	 * suitable program could be found.
	 * @param names The array of program names to try.
	 * @return The first supported program from the array of names.
	 * @throws Exception if no supported program could be found.
	 */
	private static String findSupportedProgram(String kind, String[] names) throws Exception {
		for (String name : names) {
			Process process = Runtime.getRuntime().exec(new String[] { "which", name });
			if (process.waitFor() == 0)
				return name;
		}

		throw new Exception("Unable to find supported " + kind);
	}
	public static String deDup(final String s) {
		return new LinkedHashSet<String>(Arrays.asList(s.split("-")))
				.toString().replaceAll("(^\\[|\\]$)", "").replace(", ", "-");
	}
	/**
	 * Open a URL using java.awt.Desktop or a couple different manual methods
	 * @param url
	 * 			The URL to open
	 * @throws Exception
	 * 			If an error occurs attempting to open the url
	 */
	public static void openURL(URL url) throws Exception {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			desktop.browse(url.toURI());
		} else {
	
			switch (OSUtils.getOS()) {
				case OSX:
					Class.forName("com.apple.eio.FileManager").getDeclaredMethod(
							"openURL", new Class[] { String.class }).invoke(null,
							new Object[] { url.toString() });
					break;
				case WINDOWS:
					Runtime.getRuntime().exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", url.toString() });
					break;
				default:
					String browser = findSupportedProgram("browser", BROWSERS);
					Runtime.getRuntime().exec(new String[] { browser, url.toString() });
					break;
			}
		}
	}
	
	public static void downloadSong(final String url, final String fileName, boolean addToPlaylist, int playlistId) {
		AurousSettings settings = new AurousSettings();
		final String[] paths = settings.getScanPaths();
		if (paths[0] != null) {
			if (!paths[0].isEmpty()) {
				final Downloader downloader = new Downloader();
				Downloader.Progress progress;
				try {
					String path = PathUtils.combine(paths[0], fileName);
					path = path.substring(0,path.length()-1);
					File song = new File(path);
					progress = downloader.download(new URL(url), song,
							new DownloadResult() {

								@Override
								public void finished(final long time) {
									if (addToPlaylist) {
										try {
											Thread.sleep(500);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										final String localUrl = "file:///"
												+ AurousStringUtils.htmlEncode(song.getPath()
														.toString().replace("\\", "/"));
										int id = DatabaseManager.getIdByLocalPath(localUrl);
										if (id != -1) {
											DatabaseManager.addSongToPlaylist(id, playlistId);
										}
									}
									
								}

								@Override
								public void failed(final long time,
										final IOException status, final int response) {
									System.out.println("Donwload failed after "
											+ (int) (time / 1000)
											+ " seconds, with response code "
											+ response);
								}
							});
				} catch (final MalformedURLException ex) {
					ExceptionWidget widget = new ExceptionWidget(ex);
					widget.showWidget();
				}
			}
		}
	
	}

	public static String readFile(final String path, final Charset encoding)  {
		try {
			final byte[] encoded = Files.readAllBytes(Paths.get(path));
			return new String(encoded, encoding);
		} catch (IOException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
		return path;
	}

	public static boolean isNull(final Object obj) {
		return obj == null;
	}
}
