package com.nimbusbase.tpcsltd.twozerogame;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


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
            TYPE_BOOLEAN = "boolean",
            TYPE_STRING = "string";

    static final String SQL_CREATE = "CREATE TABLE "+ TABLE_NAME + " (" +
            FIELD_TYPE + " TEXT," + FIELD_NAME + " TEXT," + FIELD_VALUE + " TEXT," +
            "PRIMARY KEY(" + FIELD_TYPE + "," + FIELD_NAME + ")" +")";
    static final String
            COORDINATE = "COORDINATE",
            UNDO_COORDINATE = "UNDO_COORDINATE";

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
                } else if (TYPE_STRING.equals(type)) {
                    return cursor.getString(0);
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
    public Integer getCoordinate (String name, int defaultValue) {
        String coordinateString = (String) get(TYPE_STRING, COORDINATE, "");
        Map<String, Integer> coordinates = parseCoordinate(coordinateString);
        Integer v = coordinates.get(name);
        if (v == null) {
            return defaultValue;
        } else {
            return v;
        }
    }
    public Integer getUndoCoordinate (String name, int defaultValue) {
        String coordinateString = (String) get(TYPE_STRING, UNDO_COORDINATE, "");
        Map<String, Integer> coordinates = parseCoordinate(coordinateString);
        Integer v = coordinates.get(name);
        if (v == null) {
            return defaultValue;
        } else {
            return v;
        }
    }
    private Map<String, Integer> parseCoordinate (String coordinate) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        if (coordinate != null) {
            String[] coo = coordinate.split(",");
            if (coo != null && coo.length > 0) {
                for (String c : coo) {
                    String[] pair = c.split(":");
                    if (pair!=null&&pair.length==2) {
                        result.put(pair[0], Integer.valueOf(pair[1]));
                    }
                }
            }
        }
        return result;
    }

    class Editor {
        SQLiteDatabase db;
        Throwable error;
        StringBuffer coordinate = new StringBuffer();
        StringBuffer undoCoordinate = new StringBuffer();
        boolean isFirstCoordinateAppended = false;
        boolean isFirstUndoCoordinateAppended = false;
        private Editor (SQLiteDatabase db) {
            this.db = db;
            db.beginTransaction();
        }
        public void commit() {
            put(TYPE_STRING, COORDINATE, coordinate.toString());
            put(TYPE_STRING, UNDO_COORDINATE, undoCoordinate.toString());
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
                } else if (value instanceof String) {
                    values.put(FIELD_VALUE, (String) value);
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

        public void putCoordinate(String name, Integer value) {
            if (!isFirstCoordinateAppended) {
                isFirstCoordinateAppended = true;
            } else {
                coordinate.append(",");
            }
            coordinate.append(name + ":" + value);
        }
        public void putUndoCoordinate(String name, Integer value) {
            if (!isFirstUndoCoordinateAppended) {
                isFirstUndoCoordinateAppended = true;
            } else {
                undoCoordinate.append(",");
            }
            undoCoordinate.append(name + ":" + value);
        }
    }
}
