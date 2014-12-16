package com.nimbusbase.tpcsltd.twozerogame;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NimbusStorage extends SQLiteOpenHelper {
    static final int VERSION = 1;
    static final String
            NAME = "Game2048",
            // table to store data
            TABLE_SNAPSHOT = "NBTSnapshot",
            TABLE_SCORE = "NBTScore",
            // fields in the tables
            FIELD_SIZE = "size",
            FIELD_POINTS = "points",
            FIELD_SCORE = "score",
            FIELD_CREATE_AT = "createAt",
            FIELD_STATE = "state",
            FIELD_ID = "id",
            FIELD_VALUE = "value";

    static final String
            SQL_CREATE_SNAPSHOT = "CREATE TABLE "+ TABLE_SNAPSHOT + " (" +
            FIELD_SIZE + " INTEGER," + FIELD_POINTS + " TEXT," + FIELD_SCORE + " INTEGER," +
            FIELD_CREATE_AT + " INTEGER," + FIELD_STATE + " INTEGER," + FIELD_ID + " INTEGER PRIMARY KEY" +
            ");\n",
            SQL_CREATE_SCORE = "CREATE TABLE "+ TABLE_SCORE + " (" +
            FIELD_VALUE + " INTEGER," + FIELD_CREATE_AT + " INTEGER," + FIELD_ID + " INTEGER PRIMARY KEY" +
            ");\n";

    SQLiteDatabase readableDb;

    public NimbusStorage(Context context) {
        super(context, NAME, null, VERSION);
        this.readableDb = this.getReadableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SNAPSHOT);
        db.execSQL(SQL_CREATE_SCORE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public List<Snapshot> getSnapshots () {
        List<Snapshot> result = new ArrayList<Snapshot>();
        SQLiteDatabase db = readableDb.isOpen()?readableDb:getReadableDatabase();
        Cursor cursor = db.query(TABLE_SNAPSHOT, null,null, null, null, null, FIELD_CREATE_AT+" DESC");
        if (cursor.moveToFirst()) {
            do {
                Snapshot snapshot = new Snapshot();
                snapshot.setSize(cursor.getInt(cursor.getColumnIndex(FIELD_SIZE)));
                snapshot.setPoints(cursor.getString(cursor.getColumnIndex(FIELD_POINTS)));
                snapshot.setScore(cursor.getLong(cursor.getColumnIndex(FIELD_SCORE)));
                snapshot.setCreateAt(cursor.getLong(cursor.getColumnIndex(FIELD_CREATE_AT)));
                snapshot.setState(cursor.getInt(cursor.getColumnIndex(FIELD_STATE)));
                snapshot.setId(cursor.getLong(cursor.getColumnIndex(FIELD_ID)));
                result.add(snapshot);
            } while (cursor.moveToNext());
        }
        return result;
    }

    public Long getHighScore(long defaultValue) {
        String sql = "SELECT MAX("+FIELD_VALUE+") FROM "+TABLE_SCORE+";";
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            } else {
                return defaultValue;
            }
        } finally {
            db.close();
        }
    }

    public void clearHighScoreTable () {
        String sql = "DELETE FROM " + TABLE_SCORE + " WHERE " + FIELD_VALUE + "<" +
                "(SELECT MAX(value) FROM "+TABLE_SCORE+" );";
        SQLiteDatabase writableDb = getWritableDatabase();
        try {
            writableDb.execSQL(sql);
        } finally {
            writableDb.close();
        }
    }

    public void clearSnapshotTable () {
        SQLiteDatabase writableDb = getWritableDatabase();
        List<Snapshot> snapshots = getSnapshots();
        if (snapshots.size()<=3) {
            return;
        }
        try {
            writableDb.delete(TABLE_SNAPSHOT, FIELD_CREATE_AT + "<?",
                    new String[]{String.valueOf(snapshots.get(2).getCreateAt())});
        } finally {
            writableDb.close();
        }
    }

    public long recordHighScore(long highScore) {
        ContentValues values = new ContentValues();
        values.put(FIELD_CREATE_AT, new Date().getTime());
        values.put(FIELD_VALUE, highScore);
        SQLiteDatabase writableDb = getWritableDatabase();
        long result = 0;
        try {
            result = writableDb.insert(TABLE_SCORE, null, values);
        } finally {
            writableDb.close();
        }
        return result;
    }

    public void close() {
        this.readableDb.close();
    }


    public  Snapshot newSnapshot() {
        return new Snapshot();
    }

    class Snapshot {
        int size;
        String points;
        long score;
        long createAt;
        int state;
        long id;
        Map<String, Integer> pointsHolder;

        public Snapshot() {
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getPoints() {
            if (points == null) {
                return joinPoints();
            } else {
                return points;
            }
        }

        public void setPoints(String points) {
            this.points = points;
        }

        public long getScore() {
            return score;
        }

        public void setScore(long score) {
            this.score = score;
        }

        public long getCreateAt() {
            return createAt;
        }

        public void setCreateAt(long createAt) {
            this.createAt = createAt;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public CoordinateReader pointsReader() {
            return new CoordinateReader(points, size);
        }

        public void putPoint(String coordinate, int value) {
            if (pointsHolder == null) {
                pointsHolder = new HashMap<String, Integer>();
            }
            pointsHolder.put(coordinate, value);
        }

        private String joinPoints () {
            if (pointsHolder.entrySet().size() != size*size) {
                throw new AssertionError("points are not illegal:"+pointsHolder.toString());
            }
            StringBuffer sb = new StringBuffer();
            for (int i=0; i<size; i++) {
                for (int j=0; j<size; j++) {
                    if (i!=0 || j!=0) {
                        sb.append(",");
                    }
                    Integer value = pointsHolder.get(i + " " + j);
                    if (value == null) {
                        throw new AssertionError("point are not illegal:"+value);
                    }
                    sb.append(pointsHolder.get(i+" "+j));
                }
            }
            return sb.toString();
        }

        public boolean equals(Snapshot another) {
            if (another == null) {
                return false;
            }
            if (!getPoints().equals(another.getPoints())) {
                return false;
            }
            if (!new Integer(getSize()).equals(another.getSize())) {
                return false;
            }
            if (!new Long(getScore()).equals(another.getScore())) {
                return false;
            }
            if (!new Integer(getState()).equals(another.getState())) {
                return false;
            }
            return true;
        }
        public long create() {
            ContentValues values = new ContentValues();
            values.put(FIELD_SIZE, getSize());
            values.put(FIELD_POINTS, joinPoints());
            values.put(FIELD_SCORE, getScore());
            values.put(FIELD_CREATE_AT, getCreateAt());
            values.put(FIELD_STATE, getState());
            SQLiteDatabase db = getWritableDatabase();
            long result = -1;
            try {
                result = db.insert(TABLE_SNAPSHOT, null, values);
            } finally {
                db.close();
            }
            return result;
        }
    }

    class CoordinateReader {
        Map<String, Integer> coordinates;
        int size;

        private CoordinateReader(String points, int size) {
            this.size = size;
            coordinates = parseCoordinate(points);

        }

        public Integer getCoordinate (String name, int defaultValue) {
            Integer v = coordinates.get(name);
            if (v == null) {
                return defaultValue;
            } else {
                return v;
            }
        }

        private Map<String, Integer> parseCoordinate (String coordinate) {
            Map<String, Integer> result = new HashMap<String, Integer>();
            int counter = 0;
            if (coordinate != null) {
                String[] coo = coordinate.split(",");
                if (coo != null && coo.length == size*size) {
                    for (String c : coo) {
                        int col = counter % size;
                        int row = counter / size;
                        result.put(row+" "+col, Integer.valueOf(c));
                        counter++;
                    }
                }
            }
            return result;
        }
    }
}
