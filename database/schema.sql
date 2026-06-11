-- Schema Definition for Room Database (user_profiles table)
-- Database Version: 5
-- Target Database Engine: SQLite

-- Drops table if exists to ensure a clean setup (useful for setup scripts)
DROP TABLE IF EXISTS `user_profiles`;

-- Definition of user_profiles table
CREATE TABLE IF NOT EXISTS `user_profiles` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `profile_name` TEXT NOT NULL DEFAULT '',
    `full_name` TEXT NOT NULL DEFAULT '',
    `email` TEXT NOT NULL DEFAULT '',
    `phone_number` TEXT NOT NULL DEFAULT '',
    `address` TEXT NOT NULL DEFAULT '',
    `custom_fields` TEXT NOT NULL DEFAULT '{}',
    `sections` TEXT NOT NULL DEFAULT '[]'
);

-- Index recommendations for query optimizations (if performing lookup by profile name)
CREATE UNIQUE INDEX IF NOT EXISTS `index_user_profiles_profile_name` ON `user_profiles` (`profile_name`);
