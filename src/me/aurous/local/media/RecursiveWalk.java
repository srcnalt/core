package me.aurous.local.media;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.RecursiveAction;

import me.aurous.exceptions.ExceptionWidget;
import me.aurous.local.database.DatabaseManager;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

public class RecursiveWalk extends RecursiveAction {

	private static final long serialVersionUID = 6913234076030245489L;
	private final Path dir;
	String[] types = { "flac", "mp3", "wav", "opus", "weba", // audio types
			"ogg" };

	/*
	 * try { final PreparedStatement prep = DatabaseManager.connection
	 * .prepareStatement(
	 * "insert into songs  (title, artist, album, albumArt, duration, localPath, remotePath, concave) values (?, ?, ?, ?, ?, ?, ?, ?);"
	 * ); DatabaseManager.connection.setAutoCommit(false); Parallel.ForJoin(
	 * songs, param -> {
	 *
	 * final File song = new File(param); AudioFile f; final String localUrl =
	 * "file:///" + AurousStringUtils.htmlEncode(song.getPath()
	 * .toString().replace("\\", "/")); if
	 * (!DatabaseManager.isInCollection(localUrl)) { try {
	 *
	 * f = AudioFileIO.read(song); final Tag tag = f.getTag(); if (tag != null)
	 * { String artist = tag.getFirst(FieldKey.ARTIST); String title =
	 * tag.getFirst(FieldKey.TITLE); String album =
	 * tag.getFirst(FieldKey.ALBUM); if (StringUtils.isBlank(artist)) { artist =
	 * "Unknown"; } if (StringUtils.isBlank(title)) { title = "Unknown"; } if
	 * (StringUtils.isBlank(album)) { album = "Unknown"; } final int duration =
	 * f.getAudioHeader() .getTrackLength(); try {
	 *
	 *
	 * prep.setString(1, title); prep.setString(2, artist); prep.setString(3,
	 * album); prep.setString(4, "https://i.imgur.com/WlXQJZK.png");
	 * prep.setInt(5, duration); prep.setString(6, localUrl); prep.setString(7,
	 * "null"); prep.setString(8, "null");
	 *
	 * prep.addBatch();
	 *
	 *
	 * } catch (final Exception e) {
	 *
	 * } }
	 *
	 * } catch (CannotReadException | IOException | TagException |
	 * ReadOnlyFileException | InvalidAudioFrameException e) {
	 *
	 * } } });
	 *
	 * prep.executeBatch(); DatabaseManager.connection.setAutoCommit(true);
	 * browser.executeJavaScript("mediaScanner.completeScanning();"); } catch
	 * (final SQLException e1) { // TODO Auto-generated catch block
	 * e1.printStackTrace(); }
	 */
	public RecursiveWalk(final Path dir) {
		this.dir = dir;
	}

	private String getExtension(final String path) {
		final int dot = path.lastIndexOf(".");
		return path.substring(dot + 1);
	}

	private void addSong(final Path fileName) {

	}

	@Override
	protected void compute() {
		final List<RecursiveWalk> walks = new ArrayList<>();
		try {
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(final Path dir,
						final BasicFileAttributes attrs) throws IOException {
					if (!dir.equals(RecursiveWalk.this.dir)) {
						final RecursiveWalk w = new RecursiveWalk(dir);
						w.fork();
						walks.add(w);
						return FileVisitResult.SKIP_SUBTREE;
					} else {
						return FileVisitResult.CONTINUE;
					}
				}

				String htmlEncode(final String string) {
					final StringBuffer stringBuffer = new StringBuffer();
					for (int i = 0; i < string.length(); i++) {
						final Character character = string.charAt(i);
						if (CharUtils.isAscii(character)) {
							// Encode common HTML equivalent characters
							stringBuffer.append(StringEscapeUtils
									.escapeHtml4(character.toString()));
						} else {
							// Why isn't this done in escapeHtml4()?
							stringBuffer.append(String.format("&#x%x;",
									Character.codePointAt(string, i)));
						}
					}
					return stringBuffer.toString();
				}

				@Override
				public FileVisitResult visitFile(final Path file,
						final BasicFileAttributes attrs) throws IOException {
					// System.out.println(file + "\t" + Thread.currentThread());
					// addSong(file);
					final File song = file.toFile();
					AudioFile f;
					final String extension = getExtension(song.getPath());
					if (Arrays.asList(types).contains(extension)) {
						final String localUrl = "file:///  "
								+ htmlEncode(song.getPath());
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

								final String artworkBase64 = "https://i.imgur.com/WlXQJZK.png";
								if (!DatabaseManager.isInCollection(localUrl)) {
									DatabaseManager.addSong(title, artist,
											album, artworkBase64, duration,
											localUrl, "null", "null");
								//	System.out.println(localUrl);
								}
							} else {
								/*
								 * final int duration = f.getAudioHeader()
								 * .getTrackLength(); if
								 * (!DatabaseManager.isInCollection(localUrl)) {
								 * DatabaseManager.addSong("Unknown", "Unknown",
								 * "Unknown", "https://i.imgur.com/WlXQJZK.png",
								 * duration, localUrl, "null", "null");; }
								 */

							}
						} catch (CannotReadException | IOException
								| TagException | ReadOnlyFileException
								| InvalidAudioFrameException e) {
							ExceptionWidget widget = new ExceptionWidget(e);
							widget.showWidget();
						}
					}

					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final IOException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}

		for (final RecursiveWalk w : walks) {
			w.join();
		}
	}

}