package me.aurous.local.media.watcher;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.aurous.exceptions.ExceptionWidget;
import me.aurous.local.database.DatabaseManager;
import me.aurous.local.settings.AurousSettings;
import me.aurous.utils.AurousStringUtils;

import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import com.teamdev.jxbrowser.chromium.Browser;

public class WatcherService {

	private Browser browser;
	public WatcherService(Browser browser) {
		this.browser = browser;
	}
	public void startService() {
		final AurousSettings settings = new AurousSettings();
		final String[] paths = settings.getScanPaths();
		if ((paths != null) && (paths.length > 0)) {
			if (!paths[0].isEmpty()) {
				try {

					final SimpleWatcher watcher = new SimpleWatcher(paths);
					final String[] types = { "flac", "mp3", "wav", "opus",
							"weba", // audio types
					"ogg" };
					watcher.addExtensions(types); // etc (also accepts arrays)
					watcher.addWatcher(new FileChangeWatcher() {

						@Override
						public void added(final File p) {
							System.out.println("File added: " + p);
							final File song = p;
							AudioFile f;

							final String localUrl = "file:///"
									+ AurousStringUtils.htmlEncode(song.getPath()
											.toString().replace("\\", "/"));
							try {
								if (!DatabaseManager.isInCollection(localUrl)) {

									Logger.getLogger("org.jaudiotagger").setLevel(
											Level.OFF);
									f = AudioFileIO.read(song);
									final Tag tag = f.getTag();
									if (tag != null) {
										String artist = tag
												.getFirst(FieldKey.ARTIST);
										String title = tag.getFirst(FieldKey.TITLE);
										String album = tag.getFirst(FieldKey.ALBUM);
										String albumart = "assets/img/noalbumart.png";
										if (StringUtils.isBlank(artist)) {
											artist = "Unknown";
										}
										if (StringUtils.isBlank(title)) {
											title = "Unknown";
										}
										if (StringUtils.isBlank(album)) {
											album = "Unknown";
										}
										final int duration = f.getAudioHeader()
												.getTrackLength();

										DatabaseManager.addSong(title, artist, album, albumart, duration, localUrl, "null",  "null");
										int id = DatabaseManager.getIdByLocalPath(localUrl);
										if (id != -1) {
											String script = "songCollection.appendDynamicRow(%s, '%s', %s, '%s', '%s', '%s', '%s');";
											script = String.format(script, id, albumart, duration, album, title, artist, localUrl );
											browser.executeJavaScript(script);
								//			System.out.println("song added");
										}
									}
								}
							} catch (SecurityException | KeyNotFoundException
									| CannotReadException | IOException
									| TagException | ReadOnlyFileException
									| InvalidAudioFrameException e) {
								//ExceptionWidget widget = new ExceptionWidget(e);
							//	widget.showWidget();
							}
						}

						@Override
						public void removed(final File p) {
							System.out.println("File removed: " + p);
							final String localUrl = "file:///"
									+ AurousStringUtils.htmlEncode(p.getPath()
											.toString().replace("\\", "/"));
							int id = DatabaseManager.getIdByLocalPath(localUrl);
							DatabaseManager.removeSong(p.toString());
							if (id != -1 ) {
							String script = "songCollection.removeCollectionRow(%s);";
							script = String.format(script, id);
							browser.executeJavaScript(script);
						//	System.out.println(id);
							} else {
						//		System.out.println("No song present");
							}
						}

						@Override
						public void moved(final File from, final File to) {
						//	System.out.println("File moved: " + from.toString()
					//				+ " -> " + to.toString());
							final File song = to;
							AudioFile f;

							final String localUrl = "file:///"
									+ AurousStringUtils.htmlEncode(song.getPath()
											.toString().replace("\\", "/"));
							try {
								if (!DatabaseManager.isInCollection(localUrl)) {

									Logger.getLogger("org.jaudiotagger").setLevel(
											Level.OFF);
									f = AudioFileIO.read(song);
									final Tag tag = f.getTag();
									if (tag != null) {
										String artist = tag
												.getFirst(FieldKey.ARTIST);
										String title = tag.getFirst(FieldKey.TITLE);
										String album = tag.getFirst(FieldKey.ALBUM);
										String albumart = "assets/img/noalbumart.png";
										if (StringUtils.isBlank(artist)) {
											artist = "Unknown";
										}
										if (StringUtils.isBlank(title)) {
											title = "Unknown";
										}
										if (StringUtils.isBlank(album)) {
											album = "Unknown";
										}
										final int duration = f.getAudioHeader()
												.getTrackLength();

										DatabaseManager.addSong(title, artist, album, albumart, duration, localUrl, "null",  "null");
										int id = DatabaseManager.getIdByLocalPath(localUrl);
										if (id != -1) {
											String script = "songCollection.appendDynamicRow(%s, '%s', %s, '%s', '%s', '%s', '%s');";
											script = String.format(script, id, albumart, duration, album, title, artist, localUrl );
											browser.executeJavaScript(script);
											browser.executeJavaScript("songCollection.needsUpdate = true;");
									//		System.out.println("song added");
										}
									}
								}
							} catch (SecurityException | KeyNotFoundException
									| CannotReadException | IOException
									| TagException | ReadOnlyFileException
									| InvalidAudioFrameException e) {
								ExceptionWidget widget = new ExceptionWidget(e);
								widget.showWidget();
							}
							
						}

					});

				} catch (final IOException ex) {
					ExceptionWidget widget = new ExceptionWidget(ex);
					widget.showWidget();
				}
			}
		}
	}
}
