package com.example.allvideodownloader.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.allvideodownloader.database.CommandTemplate
import com.example.allvideodownloader.database.DownloadedVideoInfo
import com.example.allvideodownloader.database.VideoInfoDao

@Database(
    entities = [DownloadedVideoInfo::class, CommandTemplate::class], version = 3, autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoInfoDao(): VideoInfoDao
}