package com.notakeyboard.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Base database
 */
public class BaseDB extends SQLiteOpenHelper {
  private static final String NAME = "NOTA.db";
  private static final int version = 17;

  // table names
  static final String KEY_SPEC_TABLE = "key_spec";
  static final String PREFERENCES_TABLE = "preference";

  protected SQLiteDatabase db;
  protected Context context;

  public BaseDB(Context context) {
    super(context, NAME, null, version);
    this.context = context;
  }

  /**
   * Called when database is created for the first time.
   * Table creations and initial population of the tables should happen here.
   *
   * @param db database instance
   */
  @Override
  public void onCreate(SQLiteDatabase db) {
    this.db = db;
    createDB();
  }

  /**
   * Executed on database schema version upgrade.
   *
   * @param db         the database to update
   * @param oldVersion old version number
   * @param newVersion new version number
   */
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    this.db = db;
    createDB();
  }

  /**
   * Executed on database schema version downgrade.
   *
   * @param db         database
   * @param oldVersion previous version number
   * @param newVersion new version number
   */
  @Override
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    this.db = db;
    createDB();
  }

  @Override
  public void onOpen(SQLiteDatabase db) {
    super.onOpen(db);
    this.db = db;
  }

  /**
   * Delets all database upon key spec change (language setting change or keyboard spec change)
   */
  public void resetForKeySpecChange() {
    db.execSQL("DROP TABLE IF EXISTS " + KEY_SPEC_TABLE + ";");

    // then create database again
    createDB();
  }

  /**
   * Creates databases needed for this application.
   */
  private void createDB() {
    // create preferences table
    db.execSQL("CREATE TABLE IF NOT EXISTS " + PREFERENCES_TABLE
        + " (key varchar(30) PRIMARY KEY, " + "value text);");

    // create key spec table
    db.execSQL("CREATE TABLE IF NOT EXISTS " + KEY_SPEC_TABLE
        + " (type integer, keycode integer, x integer, y integer, "
        + " h integer, w integer, PRIMARY KEY(type, keycode));");
  }

}
