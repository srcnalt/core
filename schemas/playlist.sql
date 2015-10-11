CREATE TABLE `playlist` (
	`id`	INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE,
	`name`	TEXT,
	`description`	TEXT,
	`created_by`	TEXT,
	`last_updated`	NUMERIC
)