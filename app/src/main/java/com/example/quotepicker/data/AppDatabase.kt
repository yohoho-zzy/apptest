package com.example.quotepicker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromType(type: ResourceType): String = type.name

    @TypeConverter
    fun toType(raw: String): ResourceType = ResourceType.valueOf(raw)

    @TypeConverter
    fun fromCategoryType(type: TagCategoryType): String = type.name

    @TypeConverter
    fun toCategoryType(raw: String): TagCategoryType = TagCategoryType.valueOf(raw)

    @TypeConverter
    fun fromMarkState(state: ResourceMarkState): String = state.name

    @TypeConverter
    fun toMarkState(raw: String): ResourceMarkState = ResourceMarkState.valueOf(raw)
}

@Database(
    entities = [
        TagCategoryEntity::class,
        TagEntity::class,
        CharacterEntity::class,
        ResourceEntity::class,
        ResourceTagCrossRef::class,
        CharacterTagCrossRef::class,
        ResourceCharacterCrossRef::class,
        ResponseRecordEntity::class,
        ExecutionSettingsEntity::class,
        ExecutionResourceEntity::class,
        TextResourceUsageHistoryEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagCategoryDao(): TagCategoryDao
    abstract fun tagDao(): TagDao
    abstract fun characterDao(): CharacterDao
    abstract fun resourceDao(): ResourceDao
    abstract fun crossRefDao(): CrossRefDao
    abstract fun responseRecordDao(): ResponseRecordDao
    abstract fun executionSettingsDao(): ExecutionSettingsDao
    abstract fun executionResourceDao(): ExecutionResourceDao
    abstract fun textResourceUsageHistoryDao(): TextResourceUsageHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quote_picker.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
