package com.example.quotepicker.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile TagCategoryDao _tagCategoryDao;

  private volatile TagDao _tagDao;

  private volatile CharacterDao _characterDao;

  private volatile ResourceDao _resourceDao;

  private volatile CrossRefDao _crossRefDao;

  private volatile ResponseRecordDao _responseRecordDao;

  private volatile ExecutionSettingsDao _executionSettingsDao;

  private volatile ExecutionResourceDao _executionResourceDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(7) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tag_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tag_categories_type_name` ON `tag_categories` (`type`, `name`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` INTEGER NOT NULL, `name` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_categoryId_name` ON `tags` (`categoryId`, `name`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `characters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `points` INTEGER NOT NULL, `familiarity` INTEGER NOT NULL, `probability` INTEGER NOT NULL, `probabilityDate` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_characters_name` ON `characters` (`name`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `resources` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `contentUriOrPath` TEXT, `quoteText` TEXT, `quoteImageBase64` TEXT, `sceneJson` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `resource_tag_cross_ref` (`resourceId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`resourceId`, `tagId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `character_tag_cross_ref` (`characterId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`characterId`, `tagId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `resource_character_cross_ref` (`resourceId` INTEGER NOT NULL, `characterId` INTEGER NOT NULL, PRIMARY KEY(`resourceId`, `characterId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `response_records` (`characterId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, `count` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`characterId`, `tagId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `execution_settings` (`id` INTEGER NOT NULL, `buttonLabel` TEXT NOT NULL, `successToast` TEXT NOT NULL, `failureToast` TEXT NOT NULL, `pastAverage` INTEGER NOT NULL, `lastInputValue` INTEGER NOT NULL, `dailyAverage` INTEGER NOT NULL, `remainingValue` INTEGER NOT NULL, `lastExecutionDate` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `execution_resources` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `resourceId` INTEGER NOT NULL, `characterId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, `characterName` TEXT NOT NULL, `tagName` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f9eaa080846815eafbfbc5b5fbaf7985')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `tag_categories`");
        db.execSQL("DROP TABLE IF EXISTS `tags`");
        db.execSQL("DROP TABLE IF EXISTS `characters`");
        db.execSQL("DROP TABLE IF EXISTS `resources`");
        db.execSQL("DROP TABLE IF EXISTS `resource_tag_cross_ref`");
        db.execSQL("DROP TABLE IF EXISTS `character_tag_cross_ref`");
        db.execSQL("DROP TABLE IF EXISTS `resource_character_cross_ref`");
        db.execSQL("DROP TABLE IF EXISTS `response_records`");
        db.execSQL("DROP TABLE IF EXISTS `execution_settings`");
        db.execSQL("DROP TABLE IF EXISTS `execution_resources`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsTagCategories = new HashMap<String, TableInfo.Column>(5);
        _columnsTagCategories.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTagCategories.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTagCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTagCategories.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTagCategories.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTagCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTagCategories = new HashSet<TableInfo.Index>(1);
        _indicesTagCategories.add(new TableInfo.Index("index_tag_categories_type_name", true, Arrays.asList("type", "name"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoTagCategories = new TableInfo("tag_categories", _columnsTagCategories, _foreignKeysTagCategories, _indicesTagCategories);
        final TableInfo _existingTagCategories = TableInfo.read(db, "tag_categories");
        if (!_infoTagCategories.equals(_existingTagCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "tag_categories(com.example.quotepicker.data.TagCategoryEntity).\n"
                  + " Expected:\n" + _infoTagCategories + "\n"
                  + " Found:\n" + _existingTagCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsTags = new HashMap<String, TableInfo.Column>(6);
        _columnsTags.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("categoryId", new TableInfo.Column("categoryId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("colorArgb", new TableInfo.Column("colorArgb", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTags = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTags = new HashSet<TableInfo.Index>(1);
        _indicesTags.add(new TableInfo.Index("index_tags_categoryId_name", true, Arrays.asList("categoryId", "name"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoTags = new TableInfo("tags", _columnsTags, _foreignKeysTags, _indicesTags);
        final TableInfo _existingTags = TableInfo.read(db, "tags");
        if (!_infoTags.equals(_existingTags)) {
          return new RoomOpenHelper.ValidationResult(false, "tags(com.example.quotepicker.data.TagEntity).\n"
                  + " Expected:\n" + _infoTags + "\n"
                  + " Found:\n" + _existingTags);
        }
        final HashMap<String, TableInfo.Column> _columnsCharacters = new HashMap<String, TableInfo.Column>(9);
        _columnsCharacters.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCharacters.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCharacters.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCharacters.put("points", new TableInfo.Column("points", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCharacters.put("familiarity", new TableInfo.Column("familiarity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCharacters.put("probability", new TableInfo.Column("probability", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCharacters.put("probabilityDate", new TableInfo.Column("probabilityDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCharacters.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCharacters.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCharacters = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCharacters = new HashSet<TableInfo.Index>(1);
        _indicesCharacters.add(new TableInfo.Index("index_characters_name", true, Arrays.asList("name"), Arrays.asList("ASC")));
        final TableInfo _infoCharacters = new TableInfo("characters", _columnsCharacters, _foreignKeysCharacters, _indicesCharacters);
        final TableInfo _existingCharacters = TableInfo.read(db, "characters");
        if (!_infoCharacters.equals(_existingCharacters)) {
          return new RoomOpenHelper.ValidationResult(false, "characters(com.example.quotepicker.data.CharacterEntity).\n"
                  + " Expected:\n" + _infoCharacters + "\n"
                  + " Found:\n" + _existingCharacters);
        }
        final HashMap<String, TableInfo.Column> _columnsResources = new HashMap<String, TableInfo.Column>(9);
        _columnsResources.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResources.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResources.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResources.put("contentUriOrPath", new TableInfo.Column("contentUriOrPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResources.put("quoteText", new TableInfo.Column("quoteText", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResources.put("quoteImageBase64", new TableInfo.Column("quoteImageBase64", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResources.put("sceneJson", new TableInfo.Column("sceneJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResources.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResources.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysResources = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesResources = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoResources = new TableInfo("resources", _columnsResources, _foreignKeysResources, _indicesResources);
        final TableInfo _existingResources = TableInfo.read(db, "resources");
        if (!_infoResources.equals(_existingResources)) {
          return new RoomOpenHelper.ValidationResult(false, "resources(com.example.quotepicker.data.ResourceEntity).\n"
                  + " Expected:\n" + _infoResources + "\n"
                  + " Found:\n" + _existingResources);
        }
        final HashMap<String, TableInfo.Column> _columnsResourceTagCrossRef = new HashMap<String, TableInfo.Column>(2);
        _columnsResourceTagCrossRef.put("resourceId", new TableInfo.Column("resourceId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResourceTagCrossRef.put("tagId", new TableInfo.Column("tagId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysResourceTagCrossRef = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesResourceTagCrossRef = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoResourceTagCrossRef = new TableInfo("resource_tag_cross_ref", _columnsResourceTagCrossRef, _foreignKeysResourceTagCrossRef, _indicesResourceTagCrossRef);
        final TableInfo _existingResourceTagCrossRef = TableInfo.read(db, "resource_tag_cross_ref");
        if (!_infoResourceTagCrossRef.equals(_existingResourceTagCrossRef)) {
          return new RoomOpenHelper.ValidationResult(false, "resource_tag_cross_ref(com.example.quotepicker.data.ResourceTagCrossRef).\n"
                  + " Expected:\n" + _infoResourceTagCrossRef + "\n"
                  + " Found:\n" + _existingResourceTagCrossRef);
        }
        final HashMap<String, TableInfo.Column> _columnsCharacterTagCrossRef = new HashMap<String, TableInfo.Column>(2);
        _columnsCharacterTagCrossRef.put("characterId", new TableInfo.Column("characterId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCharacterTagCrossRef.put("tagId", new TableInfo.Column("tagId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCharacterTagCrossRef = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCharacterTagCrossRef = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCharacterTagCrossRef = new TableInfo("character_tag_cross_ref", _columnsCharacterTagCrossRef, _foreignKeysCharacterTagCrossRef, _indicesCharacterTagCrossRef);
        final TableInfo _existingCharacterTagCrossRef = TableInfo.read(db, "character_tag_cross_ref");
        if (!_infoCharacterTagCrossRef.equals(_existingCharacterTagCrossRef)) {
          return new RoomOpenHelper.ValidationResult(false, "character_tag_cross_ref(com.example.quotepicker.data.CharacterTagCrossRef).\n"
                  + " Expected:\n" + _infoCharacterTagCrossRef + "\n"
                  + " Found:\n" + _existingCharacterTagCrossRef);
        }
        final HashMap<String, TableInfo.Column> _columnsResourceCharacterCrossRef = new HashMap<String, TableInfo.Column>(2);
        _columnsResourceCharacterCrossRef.put("resourceId", new TableInfo.Column("resourceId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResourceCharacterCrossRef.put("characterId", new TableInfo.Column("characterId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysResourceCharacterCrossRef = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesResourceCharacterCrossRef = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoResourceCharacterCrossRef = new TableInfo("resource_character_cross_ref", _columnsResourceCharacterCrossRef, _foreignKeysResourceCharacterCrossRef, _indicesResourceCharacterCrossRef);
        final TableInfo _existingResourceCharacterCrossRef = TableInfo.read(db, "resource_character_cross_ref");
        if (!_infoResourceCharacterCrossRef.equals(_existingResourceCharacterCrossRef)) {
          return new RoomOpenHelper.ValidationResult(false, "resource_character_cross_ref(com.example.quotepicker.data.ResourceCharacterCrossRef).\n"
                  + " Expected:\n" + _infoResourceCharacterCrossRef + "\n"
                  + " Found:\n" + _existingResourceCharacterCrossRef);
        }
        final HashMap<String, TableInfo.Column> _columnsResponseRecords = new HashMap<String, TableInfo.Column>(4);
        _columnsResponseRecords.put("characterId", new TableInfo.Column("characterId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResponseRecords.put("tagId", new TableInfo.Column("tagId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResponseRecords.put("count", new TableInfo.Column("count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResponseRecords.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysResponseRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesResponseRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoResponseRecords = new TableInfo("response_records", _columnsResponseRecords, _foreignKeysResponseRecords, _indicesResponseRecords);
        final TableInfo _existingResponseRecords = TableInfo.read(db, "response_records");
        if (!_infoResponseRecords.equals(_existingResponseRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "response_records(com.example.quotepicker.data.ResponseRecordEntity).\n"
                  + " Expected:\n" + _infoResponseRecords + "\n"
                  + " Found:\n" + _existingResponseRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsExecutionSettings = new HashMap<String, TableInfo.Column>(11);
        _columnsExecutionSettings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionSettings.put("buttonLabel", new TableInfo.Column("buttonLabel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionSettings.put("successToast", new TableInfo.Column("successToast", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionSettings.put("failureToast", new TableInfo.Column("failureToast", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionSettings.put("pastAverage", new TableInfo.Column("pastAverage", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionSettings.put("lastInputValue", new TableInfo.Column("lastInputValue", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionSettings.put("dailyAverage", new TableInfo.Column("dailyAverage", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionSettings.put("remainingValue", new TableInfo.Column("remainingValue", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionSettings.put("lastExecutionDate", new TableInfo.Column("lastExecutionDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionSettings.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionSettings.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExecutionSettings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesExecutionSettings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoExecutionSettings = new TableInfo("execution_settings", _columnsExecutionSettings, _foreignKeysExecutionSettings, _indicesExecutionSettings);
        final TableInfo _existingExecutionSettings = TableInfo.read(db, "execution_settings");
        if (!_infoExecutionSettings.equals(_existingExecutionSettings)) {
          return new RoomOpenHelper.ValidationResult(false, "execution_settings(com.example.quotepicker.data.ExecutionSettingsEntity).\n"
                  + " Expected:\n" + _infoExecutionSettings + "\n"
                  + " Found:\n" + _existingExecutionSettings);
        }
        final HashMap<String, TableInfo.Column> _columnsExecutionResources = new HashMap<String, TableInfo.Column>(7);
        _columnsExecutionResources.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionResources.put("resourceId", new TableInfo.Column("resourceId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionResources.put("characterId", new TableInfo.Column("characterId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionResources.put("tagId", new TableInfo.Column("tagId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionResources.put("characterName", new TableInfo.Column("characterName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionResources.put("tagName", new TableInfo.Column("tagName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExecutionResources.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExecutionResources = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesExecutionResources = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoExecutionResources = new TableInfo("execution_resources", _columnsExecutionResources, _foreignKeysExecutionResources, _indicesExecutionResources);
        final TableInfo _existingExecutionResources = TableInfo.read(db, "execution_resources");
        if (!_infoExecutionResources.equals(_existingExecutionResources)) {
          return new RoomOpenHelper.ValidationResult(false, "execution_resources(com.example.quotepicker.data.ExecutionResourceEntity).\n"
                  + " Expected:\n" + _infoExecutionResources + "\n"
                  + " Found:\n" + _existingExecutionResources);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f9eaa080846815eafbfbc5b5fbaf7985", "e3556f2ee190825f97c220aa018c0c91");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "tag_categories","tags","characters","resources","resource_tag_cross_ref","character_tag_cross_ref","resource_character_cross_ref","response_records","execution_settings","execution_resources");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `tag_categories`");
      _db.execSQL("DELETE FROM `tags`");
      _db.execSQL("DELETE FROM `characters`");
      _db.execSQL("DELETE FROM `resources`");
      _db.execSQL("DELETE FROM `resource_tag_cross_ref`");
      _db.execSQL("DELETE FROM `character_tag_cross_ref`");
      _db.execSQL("DELETE FROM `resource_character_cross_ref`");
      _db.execSQL("DELETE FROM `response_records`");
      _db.execSQL("DELETE FROM `execution_settings`");
      _db.execSQL("DELETE FROM `execution_resources`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TagCategoryDao.class, TagCategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TagDao.class, TagDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CharacterDao.class, CharacterDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ResourceDao.class, ResourceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CrossRefDao.class, CrossRefDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ResponseRecordDao.class, ResponseRecordDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ExecutionSettingsDao.class, ExecutionSettingsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ExecutionResourceDao.class, ExecutionResourceDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TagCategoryDao tagCategoryDao() {
    if (_tagCategoryDao != null) {
      return _tagCategoryDao;
    } else {
      synchronized(this) {
        if(_tagCategoryDao == null) {
          _tagCategoryDao = new TagCategoryDao_Impl(this);
        }
        return _tagCategoryDao;
      }
    }
  }

  @Override
  public TagDao tagDao() {
    if (_tagDao != null) {
      return _tagDao;
    } else {
      synchronized(this) {
        if(_tagDao == null) {
          _tagDao = new TagDao_Impl(this);
        }
        return _tagDao;
      }
    }
  }

  @Override
  public CharacterDao characterDao() {
    if (_characterDao != null) {
      return _characterDao;
    } else {
      synchronized(this) {
        if(_characterDao == null) {
          _characterDao = new CharacterDao_Impl(this);
        }
        return _characterDao;
      }
    }
  }

  @Override
  public ResourceDao resourceDao() {
    if (_resourceDao != null) {
      return _resourceDao;
    } else {
      synchronized(this) {
        if(_resourceDao == null) {
          _resourceDao = new ResourceDao_Impl(this);
        }
        return _resourceDao;
      }
    }
  }

  @Override
  public CrossRefDao crossRefDao() {
    if (_crossRefDao != null) {
      return _crossRefDao;
    } else {
      synchronized(this) {
        if(_crossRefDao == null) {
          _crossRefDao = new CrossRefDao_Impl(this);
        }
        return _crossRefDao;
      }
    }
  }

  @Override
  public ResponseRecordDao responseRecordDao() {
    if (_responseRecordDao != null) {
      return _responseRecordDao;
    } else {
      synchronized(this) {
        if(_responseRecordDao == null) {
          _responseRecordDao = new ResponseRecordDao_Impl(this);
        }
        return _responseRecordDao;
      }
    }
  }

  @Override
  public ExecutionSettingsDao executionSettingsDao() {
    if (_executionSettingsDao != null) {
      return _executionSettingsDao;
    } else {
      synchronized(this) {
        if(_executionSettingsDao == null) {
          _executionSettingsDao = new ExecutionSettingsDao_Impl(this);
        }
        return _executionSettingsDao;
      }
    }
  }

  @Override
  public ExecutionResourceDao executionResourceDao() {
    if (_executionResourceDao != null) {
      return _executionResourceDao;
    } else {
      synchronized(this) {
        if(_executionResourceDao == null) {
          _executionResourceDao = new ExecutionResourceDao_Impl(this);
        }
        return _executionResourceDao;
      }
    }
  }
}
