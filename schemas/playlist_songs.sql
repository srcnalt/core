CREATE TABLE `songs_in_playlist` (
	`id`	INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE,
	`song_id`	INTEGER,
	`playlist_id`	INTEGER
)