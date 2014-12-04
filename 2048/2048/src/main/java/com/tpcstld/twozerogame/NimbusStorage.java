package com.tpcstld.twozerogame;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.SQLException;


public class NimbusStorage extends SQLiteOpenHelper {
    static final int VERSION = 1;
    static final String
            NAME = "NIMBUS_STORAGE",
            // table to store data
            TABLE_NAME = "game2048",
            // fields int the table
            FIELD_TYPE = "type",
            FIELD_NAME = "name",
            FIELD_VALUE = "value",
            // persistent type
            TYPE_INT = "int",
            TYPE_LONG = "long",
            TYPE_BOOLEAN = "boolean";

    static final String SQL_CREATE = "CREATE TABLE "+ TABLE_NAME + " (" +
            FIELD_TYPE + " TEXT," + FIELD_NAME + " TEXT," + FIELD_VALUE + " TEXT," +
            "PRIMARY KEY(" + FIELD_TYPE + "," + FIELD_NAME + ")" +")";

    SQLiteDatabase readableDb;
    public NimbusStorage(Context context) {
        super(context, NAME, null, VERSION);
        this.readableDb = this.getReadableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public Editor edit() {
        return new Editor(this.getWritableDatabase());
    }

    public Object get(String type, String name, Object defaultValue) {
        Cursor cursor = readableDb.query(TABLE_NAME, new String[]{FIELD_VALUE},
                FIELD_TYPE + "=? AND " + FIELD_NAME + "=?",
                new String[]{type, name}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                if (cursor.isNull(0)) {
                    return defaultValue;
                } else if (TYPE_INT.equals(type)) {
                    return cursor.getInt(0);
                } else if (TYPE_LONG.equals(type)) {
                    return cursor.getLong(0);
                } else if (TYPE_BOOLEAN.equals(type)) {
                    return cursor.getInt(0) > 0 ? true : false;
                } else {
                    return defaultValue;
                }
            } else {
                return defaultValue;
            }
        } finally {
            cursor.close();
        }
    }

    public Boolean getBoolean(String name, boolean defaultValue) {
        return (Boolean) get(TYPE_BOOLEAN, name, defaultValue);
    }

    public Integer getInt (String name, int defaultValue) {
        return (Integer) get(TYPE_INT, name, defaultValue);
    }

    public Long getLong (String name, long defaultValue) {
        return (Long) get(TYPE_LONG, name, defaultValue);
    }
    class Editor {
        SQLiteDatabase db;
        Throwable error;
        private Editor (SQLiteDatabase db) {
            this.db = db;
            db.beginTransaction();
        }
        public void commit() {
            if (error == null) {
                db.setTransactionSuccessful();
            }
            db.endTransaction();
            db.close();
        }
        public void put(String type, String name, Object value) {
            if (error != null) {
                return;
            }
            try {
                ContentValues values = new ContentValues();
                if (value == null) {
                    values.putNull(FIELD_VALUE);
                } else if (value instanceof Integer) {
                    values.put(FIELD_VALUE, (Integer) value);
                } else if (value instanceof Long) {
                    values.put(FIELD_VALUE, (Long) value);
                } else if(value instanceof Boolean) {
                    values.put(FIELD_VALUE, (Boolean) value);
                }else {
                    throw new Exception("Unsupported persistent type:"+value.getClass().getName());
                }
                long updateResult = db.update(TABLE_NAME, values,
                        FIELD_NAME + "=? AND " + FIELD_TYPE + "=?", new String[]{name, type});
                if (updateResult == 0) {
                    values.put(FIELD_TYPE, type);
                    values.put(FIELD_NAME, name);
                    long rowId = db.insertOrThrow(TABLE_NAME, null, values);
                    if (rowId < 1) {
                        error = new SQLException("An error occurred on insert.");
                    }
                } else if (updateResult != 1) {
                    error = new SQLException("An error occurred.");
                }
            } catch (Throwable t) {
                error = t;
            }
        }

        public void putBoolean(String name, Boolean value) {
            put(TYPE_BOOLEAN, name, value);
        }
        public void putInt(String name, Integer value) {
            put(TYPE_INT, name, value);
        }
        public void putLong (String name, Long value) {
            put(TYPE_LONG, name,value);
        }
    }
}
