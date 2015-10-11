package me.aurous.js;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.aurous.api.auth.impl.VKAuth;
import me.aurous.config.AppConstants;
import me.aurous.exceptions.ExceptionWidget;
import me.aurous.local.database.DatabaseManager;
import me.aurous.local.media.MediaScanner;
import me.aurous.local.settings.AurousSettings;
import me.aurous.local.settings.GSONHelper;
import me.aurous.models.search.impl.*;
import me.aurous.utils.AurousStringUtils;
import me.aurous.utils.Internet;
import me.aurous.utils.Utils;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.JSValue;

public class AurousBridge {
	Browser browser;
	Stage primaryStage;

	public AurousBridge(final Stage primaryStage, final Browser browser) {
		this.browser = browser;
		this.primaryStage = primaryStage;
	}

	private static void configureFileChooser(final FileChooser fileChooser) {
		fileChooser.setTitle("Select an Avatar");
		fileChooser.setInitialDirectory(new File(System
				.getProperty("user.home")));
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("JPG", "*.jpg"),
				new FileChooser.ExtensionFilter("PNG", "*.png"));
	}

	// disgusting, blame the JXBrowser team
	public void setupBridge() {

		browser.registerFunction("minimize", args -> {
			Platform.runLater(() -> primaryStage.setIconified(true));
			return JSValue.create(true);
		});
		browser.registerFunction("maximize", args -> {
			Platform.runLater(() -> primaryStage.setMaximized(true));
			return JSValue.create(true);
		});
		browser.registerFunction("close", args -> {
			Platform.runLater(() -> Platform.exit());

			return JSValue.create(true);
		});

		browser.registerFunction("scanMedia", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					final AurousSettings settings = new AurousSettings();
					final String[] folders = settings.getScanPaths();
					if (folders != null && folders.length > 0) {
						final MediaScanner scanner = new MediaScanner(folders);
						scanner.walkFolders(browser);
					} else {
						browser.executeJavaScript("mediaScanner.scannerErrorCallback();");
					}
				
				}
			};
			one.start();
			return JSValue.create(true);
		});
		browser.registerFunction("loadSongs", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					DatabaseManager.loadAllSongs(browser);
				}
			};
			one.start();
			return JSValue.create(true);
		});

		browser.registerFunction("checkAuths", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					final File f = new File(AppConstants.dataPath()
							+ "vkauth.dat");
					if (f.exists() && !f.isDirectory()) {
						browser.executeJavaScript("settings.setAuthButtons(true)");
					} else {
						browser.executeJavaScript("settings.setAuthButtons(false)");
						;
					}
				}
			};
			one.start();
			return JSValue.create(true);
		});
		browser.registerFunction(
				"selectProfilePicture",
				args -> {
					Platform.runLater(() -> {

						final FileChooser chooser = new FileChooser();
						configureFileChooser(chooser);

						final File file = chooser.showOpenDialog(primaryStage);
						if (file != null) {
							final String protocol = "file:///";
							String path = file.getAbsolutePath().replace("\\",
									"/");
							path = AurousStringUtils.UTFEncode(path);

							final String builtPath = protocol
									+ path.replace("%2F", "/").replace("%3A",
											":");
							browser.executeJavaScript("settings.setAvatar('"
									+ builtPath + "');");

						}

					});
					return JSValue.create(true);
				});
		browser.registerFunction("deleteVK", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					final File f = new File(AppConstants.dataPath()
							+ "vkauth.dat");
					if (f.exists() && !f.isDirectory()) {
						f.delete();
						browser.executeJavaScript("settings.setAuthButtons(false)");
					}
				}
			};
			one.start();
			return JSValue.create(true);
		});

		browser.registerFunction("authVK", args -> {

			Platform.runLater(() -> {
				final VKAuth auth = new VKAuth();
				auth.start(primaryStage, browser);
			});

			return JSValue.create(true);
		});
		browser.registerFunction("saveSettings", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					final AurousSettings settings = new AurousSettings();
					final GSONHelper helper = new GSONHelper();
					final String[] scanExcludes = args[0].getString()
							.split(";");
					final String[] scanRestrictions = args[1].getString()
							.split(";");
					final String[] scanPaths = args[2].getString().split(
							",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
					for (int i = 0; i < scanPaths.length; i++) {
						if (scanPaths[i] != null) {
							scanPaths[i] = scanPaths[i].replace("\"", "");
						}
					}

					final String searchEngine = args[3].getString();
					final String displayName = args[4].getString();
					final String avatarPath = args[5].getString();

					settings.saveSettings(avatarPath, displayName,
							searchEngine, scanPaths, scanRestrictions,
							scanExcludes);

					try {
						helper.saveClass(new FileOutputStream(
								settings.settingsPath), settings);
					} catch (final IOException e) {
						ExceptionWidget widget = new ExceptionWidget(e);
						widget.showWidget();
					}
					
					if (scanPaths != null && scanPaths.length > 0) {
					final MediaScanner scanner = new MediaScanner(scanPaths);
					scanner.walkFolders(browser);
					}
				}
			};
			one.start();
			return JSValue.create(true);
		});
		browser.registerFunction("loadSettings", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					final AurousSettings settings = new AurousSettings();
					settings.loadSettings(browser);
				}
			};
			one.start();
			return JSValue.create(true);
		});
		browser.registerFunction("downloadSongAsync", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					final String url = args[0].getString();
					String fileName = args[1].getString();
					if (fileName.contains("?extra=")) {
						fileName = fileName.split("\\?extra=")[0];
					}
			      Utils.downloadSong(browser, url, fileName);
				//	System.out.println(fileName);
				//	Utils.test(url, fileName);
				}
			};
			one.start();
			return JSValue.create(true);
		});
		browser.registerFunction("openUrl", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
				try {
					String url = args[0].getString();
					Utils.openURL(new URL(url));
				} catch (Exception e) {
					ExceptionWidget widget = new ExceptionWidget(e);
					widget.showWidget();
				}
				}
			};
			one.start();
			return JSValue.create(true);
		});
		browser.registerFunction("updateDatabaseArt", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					final String albumArt = args[0].getString();
					final int id = Integer.parseInt(args[1].getString());
					DatabaseManager.updateAlbumArt(albumArt, id);
				}
			};
			one.start();
			return JSValue.create(true);
		});

		browser.registerFunction("sortArtistDb", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					DatabaseManager.sortByArtist(browser);
				}
			};
			one.start();
			return JSValue.create(true);
		});
		browser.registerFunction("sortAlbumDb", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					DatabaseManager.sortByAlbum(browser);
				}
			};
			one.start();
			return JSValue.create(true);
		});
		browser.registerFunction("coreSearch", args -> {
			final Thread one = new Thread() {
				@Override
				public void run() {
					AurousSettings settings = new AurousSettings();
					if (settings != null) {
						String engine = settings.getSearchEngine();
						if (engine != null) {
						String phrase = args[0].getString();
						
							switch(engine) {
							case "Aurous Network":
								final Pleer pleer = new Pleer(browser, phrase);
								// final MP3WithMe appSearch = new MP3WithMe(browser,
								// args[0].getString());
								pleer.search();
								break;
							case "VK":
								final VK vk = new VK(browser, phrase);
								vk.search();
								break;
							case "MP3WithMe":
								final MP3WithMe mp3withme = new MP3WithMe(browser, phrase);
								mp3withme.search();
								break;
							}
							phrase = AurousStringUtils.UTFEncode(phrase);
							Internet.openUrl("https://aurous.me/api/terms/?phrase=" + phrase);
							
						}  else {
							String phrase = args[0].getString();
							final Pleer pleer = new Pleer(browser, phrase);
							pleer.search();
							phrase = AurousStringUtils.UTFEncode(phrase);
							Internet.openUrl("https://aurous.me/api/terms/?phrase=" + phrase);
					
						}
					}
				}
			};
			one.start();
			return JSValue.create(true);
		});

	}

}
