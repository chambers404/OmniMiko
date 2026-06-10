package com.omnimiko.data.conversation

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, MemoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OmniDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        fun build(context: Context): OmniDatabase =
            Room.databaseBuilder(context, OmniDatabase::class.java, "omnimiko.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
