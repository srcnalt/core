CREATE TABLE `songs` (
	`id`	INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE,
	`title`	TEXT,
	`artist`	TEXT,
	`album`	TEXT,
	`albumArt`	TEXT,
	`duration`	INTEGER,
	`localPath`	TEXT,
	`remotePath`	TEXT,
	`concave`	TEXT
)