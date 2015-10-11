package me.aurous.local.media;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.aurous.exceptions.ExceptionWidget;
import me.aurous.local.database.DatabaseManager;
import me.aurous.parallel.Parallel;
import me.aurous.utils.AurousStringUtils;

import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import com.teamdev.jxbrowser.chromium.Browser;

public class MediaScanner {
	private final String[] paths;
	PathMatcher matcher;
	final Collection<String> songs = new LinkedList<String>();
	String[] types = { "flac", "mp3", "wav", "opus", "weba", // audio types
	"ogg" };

	public MediaScanner(final String[] paths) {
		this.paths = paths;
	}

	public void walkFolders(final Browser browser) {
		for (final String path : paths) {
			try {
				final Path rootPath = Paths.get(path);
				Files.walkFileTree(rootPath, new AudioFileVistor());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		browser.executeJavaScript("mediaScanner.completeScanning();");
	}

	class AudioFileVistor extends SimpleFileVisitor<Path> {

		PathMatcher matcher;

		public AudioFileVistor() {

			matcher = FileSystems.getDefault().getPathMatcher(
					"glob:*.{flac,mp3,wav,opus,weba,ogg}");

		}

		@Override
		public FileVisitResult visitFile(final Path file,
				final BasicFileAttributes attrs) throws IOException {
			// System.out.println(file + "\t" + Thread.currentThread());
			// addSong(file);
			final File song = file.toFile();
			AudioFile f;
			Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
			final String extension = getExtension(song.getPath());
			if (Arrays.asList(types).contains(extension)) {
				final String localUrl = "file:///"
						+ AurousStringUtils.htmlEncode(song.getPath()
								.toString().replace("\\", "/"));
				try {

					f = AudioFileIO.read(song);
					final Tag tag = f.getTag();
					if (tag != null) {
						String artist = tag.getFirst(FieldKey.ARTIST);
						String title = tag.getFirst(FieldKey.TITLE);
						String album = tag.getFirst(FieldKey.ALBUM);
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

						final String artworkBase64 = "assets/img/noalbumart.png";
						if (!DatabaseManager.isInCollection(localUrl)) {
							DatabaseManager.addSong(title, artist,
									album, artworkBase64, duration,
									localUrl, "null", "null");
						//	System.out.println(localUrl);
						}
					} else {
						 final int duration = f.getAudioHeader().getTrackLength();
						if (!DatabaseManager.isInCollection(localUrl)) {
							DatabaseManager.addSong("Unknown", "Unknown",
									"Unknown", "assets/img/noalbumart.png", duration,
									localUrl, "null", "null");
						}
					}
				} catch (CannotReadException | IOException
						| TagException | ReadOnlyFileException
						| InvalidAudioFrameException e) {
					System.out.println("No headers detected");
				//	ExceptionWidget widget = new ExceptionWidget(e);
				//	widget.showWidget();
				}
			}
			return FileVisitResult.CONTINUE;
		}

	}

	public String getExtension(final String path) {
		final int dot = path.lastIndexOf(".");
		return path.substring(dot + 1);
	}

}