package me.aurous.local.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import me.aurous.config.AppConstants;
import me.aurous.exceptions.ExceptionWidget;
import me.aurous.utils.AurousStringUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.teamdev.jxbrowser.chromium.Browser;

public class DatabaseManager {
	public static Connection connection;
	static boolean needsCreation = false;

	public static void connect() {
		final String dbPath = AppConstants.dataPath() + "aurous.db";
		final File f = new File(dbPath);
		if (!f.exists() && !f.isDirectory()) {
			needsCreation = true;
		}
		connection = null;
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
		} catch (final Exception e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
			System.exit(0);
		}
	//	System.out.println("Opened database successfully");
		if (needsCreation) {
			createTables();

		}
	}

	public static void createTables() {
		try {
			final Statement stat = connection.createStatement();
			stat.executeUpdate("CREATE TABLE `scan_paths` (`id`	INTEGER PRIMARY KEY AUTOINCREMENT, `path`	TEXT UNIQUE)");
			stat.executeUpdate("CREATE TABLE `playlist` ( `id`	INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE, `name`	TEXT, `description`	TEXT, `created_by`	TEXT, `last_updated`	NUMERIC)");
			stat.executeUpdate("CREATE TABLE `songs` (`id`	INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE,`title`	TEXT, `artist` TEXT,`album` TEXT,`albumArt` TEXT,`duration`	INTEGER,`localPath`	TEXT,`remotePath`	TEXT,`concave` TEXT)");
			stat.executeUpdate("CREATE TABLE `songs_in_playlist` ( `id`	INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE, `song_id`	INTEGER,`playlist_id`	INTEGER)");
			stat.close();
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
	}

	public static void createPlaylist(final String name) {
		try {

			final PreparedStatement prep = connection
					.prepareStatement("insert into playlist  (name) values (?);");
			prep.setString(1, name);
			prep.addBatch();
			prep.executeBatch();
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
	}
	
	public static void updateAlbumArt(final String url, final int id) {
		try {
			final PreparedStatement prep = connection
					.prepareStatement("UPDATE songs SET albumArt = ?  WHERE id = ?; ");
			prep.setString(1, url);
			prep.setInt(2, id);
			prep.executeUpdate();
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
	}
	
	public static void removeSong(String path) {
		try {
			final PreparedStatement prep = connection
					.prepareStatement("DELETE FROM songs WHERE localPath LIKE ?;");
			path = AurousStringUtils.htmlEncode(path);
			path = path.replace("\\", "/");
			prep.setString(1, "%" + path + "%");
			prep.executeUpdate();
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void loadSongsInPlaylist(Browser browser, int index) {
		try {
			final JSONObject results = new JSONObject();
			final JSONArray resultsArray = new JSONArray();
			final Statement stat = connection.createStatement();
			final ResultSet rs = stat.executeQuery("SELECT songs.*, playlist.name as `playlist_title` FROM songs AS songs JOIN songs_in_playlist AS playlist_songs ON songs.id = playlist_songs.song_id JOIN playlist ON playlist_songs.playlist_id = playlist.id WHERE playlist.id = " + index);
			while (rs.next()) {

				final int id = rs.getInt("id");
				String title = rs.getString("title");
				if (rs.wasNull()) {
					continue;
				}
				title = AurousStringUtils.UTFEncode(title);
				final String artist = AurousStringUtils.UTFEncode(rs
						.getString("artist"));
				final String album = AurousStringUtils.UTFEncode(rs
						.getString("album"));
				final String albumArt = rs.getString("albumArt");
				final int duration = rs.getInt("duration");
				String localPath = null;
				localPath = rs.getString("localPath");
				final JSONObject resultsObject = new JSONObject();
				resultsObject.put("id", id);
				resultsObject.put("title", title);
				resultsObject.put("artist", artist);
				resultsObject.put("album", album);
				resultsObject.put("albumArt", albumArt);
				resultsObject.put("duration", duration);
				resultsObject.put("link", localPath);
				resultsArray.add(resultsObject);

			}
			results.put("results", resultsArray);
			final String bytesEncoded = Base64.encode(results.toJSONString()
					.getBytes());

			String script = "playlist.populatePlaylist('%s');";
			script = String.format(script, bytesEncoded);
			script = script.replaceAll("[\r\n]+", " ");
			// System.out.println(results.toJSONString());
			browser.executeJavaScript(script);
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}

	}
	public static void removePlaylist(int id) {
		try {
		 PreparedStatement prep = connection
					.prepareStatement("DELETE FROM playlist WHERE id = ?;");
			prep.setInt(1, id);
			prep.executeUpdate();
			
			prep = connection
					.prepareStatement("DELETE FROM songs_in_playlist WHERE playlist_id = ?;");
			prep.setInt(1, id);
			prep.executeUpdate();
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
	}
	
	public static void updateRemotePath(String path, int id) {
		try {
			final PreparedStatement prep = connection
					.prepareStatement("UPDATE songs SET remotePath = ? WHERE id =  ?;");
			path = AurousStringUtils.htmlEncode(path);
			path = path.replace("\\", "/");
			prep.setString(1,  path );
			prep.setInt(2, id);
			prep.executeUpdate();
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
	}

	public static boolean isInCollection(final String path) {
		if (connection != null) {
			try {
				final PreparedStatement prep = connection
						.prepareStatement("select * from songs WHERE localPath = ? ");
				prep.setString(1, path);
				final ResultSet rs = prep.executeQuery();
				final boolean hasNext = rs.next();
				if (!hasNext) {
					rs.close();
					return false;
				} else {
					rs.close();
					return true;
				}
			} catch (final SQLException e) {
				ExceptionWidget widget = new ExceptionWidget(e);
				widget.showWidget();
				return false;
			}
		}
		return true;
	}


	@SuppressWarnings("unchecked")
	public static String loadAllPlaylist(final Browser browser ) {
		try {
			final JSONObject results = new JSONObject();
			final JSONArray resultsArray = new JSONArray();
			final Statement stat = connection.createStatement();
			final ResultSet rs = stat.executeQuery("SELECT * from playlist;");
			while (rs.next()) {

			
				String name = rs.getString("name");
				if (rs.wasNull()) {
					continue;
				}
				int id = rs.getInt("id");
				name = AurousStringUtils.UTFEncode(name);
				final JSONObject resultsObject = new JSONObject();
				resultsObject.put("id", id);
				resultsObject.put("name", name);
				resultsArray.add(resultsObject);

			}
			results.put("results", resultsArray);
			final String bytesEncoded = Base64.encode(results.toJSONString()
					.getBytes());

			String script = "playlist.loadLocalPlaylist('%s');";
			script = String.format(script, bytesEncoded);
			script = script.replaceAll("[\r\n]+", " ");
			// System.out.println(results.toJSONString());
			browser.executeJavaScript(script);
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static String loadAllSongs(final Browser browser) {
		try {
			final JSONObject results = new JSONObject();
			final JSONArray resultsArray = new JSONArray();
			final Statement stat = connection.createStatement();
			final ResultSet rs = stat.executeQuery("SELECT * from songs;");
			while (rs.next()) {

				final int id = rs.getInt("id");
				String title = rs.getString("title");
				if (rs.wasNull()) {
					continue;
				}
				title = AurousStringUtils.UTFEncode(title);
				final String artist = AurousStringUtils.UTFEncode(rs
						.getString("artist"));
				final String album = AurousStringUtils.UTFEncode(rs
						.getString("album"));
				final String albumArt = rs.getString("albumArt");
				final int duration = rs.getInt("duration");
				String localPath = null;
				localPath = rs.getString("localPath");
				final JSONObject resultsObject = new JSONObject();
				resultsObject.put("id", id);
				resultsObject.put("title", title);
				resultsObject.put("artist", artist);
				resultsObject.put("album", album);
				resultsObject.put("albumArt", albumArt);
				resultsObject.put("duration", duration);
				resultsObject.put("link", localPath);
				resultsArray.add(resultsObject);

			}
			results.put("results", resultsArray);
			final String bytesEncoded = Base64.encode(results.toJSONString()
					.getBytes());

			String script = "songCollection.collectionCallback('%s');";
			script = String.format(script, bytesEncoded);
			script = script.replaceAll("[\r\n]+", " ");
			// System.out.println(results.toJSONString());
			browser.executeJavaScript(script);
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}

		return "";
	}

	public static String sortByAlbum(final Browser browser) {
		try {
			final JSONObject results = new JSONObject();
			final JSONArray resultsArray = new JSONArray();
			final Statement stat = connection.createStatement();
			final ResultSet rs = stat
					.executeQuery("SELECT album, artist, count(*) as 'total' FROM songs GROUP BY album");
			while (rs.next()) {

				String album = rs.getString("album");
				if (rs.wasNull()) {
					continue;
				}

				album = AurousStringUtils.UTFEncode(rs.getString("album"));
				final String artist = AurousStringUtils.UTFEncode(rs
						.getString("artist"));
				final int total = rs.getInt("total");
				final JSONObject resultsObject = new JSONObject();
				resultsObject.put("album", album);
				resultsObject.put("artist", artist);
				resultsObject.put("total", total);
				resultsArray.add(resultsObject);

			}
			results.put("results", resultsArray);
			final String bytesEncoded = Base64.encode(results.toJSONString()
					.getBytes());

			String script = "songCollection.populateAlbums('%s');";
			script = String.format(script, bytesEncoded);
			script = script.replaceAll("[\r\n]+", " ");
		//	System.out.println(results.toJSONString());
			browser.executeJavaScript(script);
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
		return "";
	}

	public static String sortByArtist(final Browser browser) {
		try {
			final JSONObject results = new JSONObject();
			final JSONArray resultsArray = new JSONArray();
			final Statement stat = connection.createStatement();
			final ResultSet rs = stat
					.executeQuery("SELECT artist, count(*) as 'total' FROM songs GROUP BY artist");
			while (rs.next()) {

				String artist = rs.getString("artist");
				if (rs.wasNull()) {
					continue;
				}

				artist = AurousStringUtils.UTFEncode(rs.getString("artist"));
				final int total = rs.getInt("total");
				final JSONObject resultsObject = new JSONObject();
				resultsObject.put("artist", artist);
				resultsObject.put("total", total);
				resultsArray.add(resultsObject);

			}
			results.put("results", resultsArray);
			final String bytesEncoded = Base64.encode(results.toJSONString()
					.getBytes());

			String script = "songCollection.populateArtist('%s');";
			script = String.format(script, bytesEncoded);
			script = script.replaceAll("[\r\n]+", " ");
		//	System.out.println(results.toJSONString());
			browser.executeJavaScript(script);
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
		return "";
	}

	public static void addSongToPlaylist(int songId, int playlistId) {
		try {

			final PreparedStatement prep = connection
					.prepareStatement("insert into songs_in_playlist (song_id, playlist_id) values (?, ?);");
			prep.setInt(1, songId);
			prep.setInt(2, playlistId);
			prep.addBatch();
			prep.executeBatch();
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
	}
	
	public static void insertSong(final String title, final String artist,
			final String album, final String albumArt, final int duration,
			final String localPath, final String remotePath,
			final String concave) {
		try {

			final PreparedStatement prep = connection
					.prepareStatement("insert into songs  (title, artist, album, albumArt, duration, localPath, remotePath, concave) values (?, ?, ?, ?, ?, ?, ?, ?);");
			prep.setString(1, title);
			prep.setString(2, artist);
			prep.setString(3, album);
			prep.setString(4, albumArt);
			prep.setInt(5, duration);
			prep.setString(6, localPath);
			prep.setString(7, remotePath);
			prep.setString(8, concave);

			prep.addBatch();
			prep.executeBatch();
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
	}

	public static int getIdByLocalPath(String path) {
		try {

			final Statement stat = connection.createStatement();
			final PreparedStatement prep = connection
					.prepareStatement("SELECT * from songs WHERE localPath = ? ;");
			prep.setString(1, path);
			
			final ResultSet rs = prep.executeQuery();
			while (rs.next()) {

				final int id = rs.getInt("id");
				if (rs.wasNull()) {
					return -1;
				} else {
					return id;
				}

			}
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}

		return -1;
	}
	public static void addSong(final String title, final String artist,
			final String album, final String albumArt, final int duration,
			final String localPath, final String remotePath,
			final String concave) {
		try {
			final PreparedStatement prep = connection
					.prepareStatement("insert into songs  (title, artist, album, albumArt, duration, localPath, remotePath, concave) values (?, ?, ?, ?, ?, ?, ?, ?);");
			prep.setString(1, title);
			prep.setString(2, artist);
			prep.setString(3, album);
			prep.setString(4, albumArt);
			prep.setInt(5, duration);
			prep.setString(6, localPath);
			prep.setString(7, remotePath);
			prep.setString(8, concave);

			prep.addBatch();
			prep.executeBatch();
		} catch (final SQLException e) {
			ExceptionWidget widget = new ExceptionWidget(e);
			widget.showWidget();
		}
	}

	public static void createSongTable() {
		try {
			final Statement stat = connection.createStatement();
			stat.executeUpdate("CREATE TABLE songs (id integer primary key autoincrement, title string, artist string, album_art string, file_path string, remote_url string);");
			stat.close();
		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

}
