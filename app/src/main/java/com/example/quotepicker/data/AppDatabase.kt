package com.example.quotepicker.data

import android.content.Context
import androidx.room.*

class Converters {
    @TypeConverter
    fun fromType(t: QuoteType): String = t.name
    @TypeConverter
    fun toType(s: String): QuoteType = QuoteType.valueOf(s)
}

@Database(entities = [GroupEntity::class, QuoteEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun quoteDao(): QuoteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quote_picker.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}