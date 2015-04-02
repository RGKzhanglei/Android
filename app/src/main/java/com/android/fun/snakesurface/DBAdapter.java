package com.android.fun.snakesurface;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DBAdapter {

    private  static final String KEY_ROWID = "_id";
    private  static final String KEY_NAME = "name";
    private  static final String KEY_SCORE = "score";
    private  static final String TAG = "DBAdapter";

    private  static final String DATABASE_NAME = "Score_DB";
    private  static final String DATABASE_TABLE = "score_history";
    private  static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE =
            "create table " + DATABASE_TABLE + "( _id integer primary key autoincrement, " +
                    KEY_NAME + " text not null, " + KEY_SCORE + " int not null);";
    private final Context context;

    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    private  static DBAdapter mInstance;

    private DBAdapter(Context cxt)
    {
        this.context = cxt;
        DBHelper = new DatabaseHelper(context);
    }

    public static DBAdapter getInstance(Context cxt) {
        if (null == mInstance) {
            mInstance = new DBAdapter(cxt);
        }
        return mInstance;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper
    {

        DatabaseHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            try
            {
                db.execSQL(DATABASE_CREATE);
            }
            catch(SQLException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
            Log.wtf(TAG, "Upgrading database from version "+ oldVersion + "to "+
                    newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    //open the database
    public DBAdapter open() throws SQLException
    {
        db = DBHelper.getWritableDatabase();
        return this;
    }
    //close the database
    public void close()
    {
        DBHelper.close();
    }

    //insert a contact into the database
    public long insertContact(String name, int score)
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        initialValues.put(KEY_SCORE, score);
        return db.insert(DATABASE_TABLE, null, initialValues);
    }
    //delete a particular contact
    public boolean deleteContact(long rowId)
    {
        return db.delete(DATABASE_TABLE, KEY_ROWID + "=" +rowId, null) > 0;
    }
    //retreves all the contacts
    public Cursor getAllContacts()
    {
        return db.query(DATABASE_TABLE, new String[]{ KEY_ROWID,KEY_NAME,KEY_SCORE}, null, null, null, null, null);
    }
    //retreves a particular contact
    public Cursor getContact(long rowId) throws SQLException
    {
        Cursor mCursor =
                db.query(true, DATABASE_TABLE, new String[]{ KEY_ROWID,
                        KEY_NAME, KEY_SCORE}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor;
    }
    //updates a contact
    public boolean updateContact(long rowId, String name, String email)
    {
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, name);
        args.put(KEY_SCORE, email);
        return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" +rowId, null) > 0;
    }

    public ArrayList<ScoreHistoryModel> getHistoryRecords() {
        ArrayList<ScoreHistoryModel> records = new ArrayList<ScoreHistoryModel>();
        open();
        Cursor cursor = getAllContacts();
        if (null != cursor) {
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                ScoreHistoryModel model = new ScoreHistoryModel();
                model.scoreName = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                model.scoreNum = cursor.getInt(cursor.getColumnIndex(KEY_SCORE));
                records.add(model);
            }

        }
        close();
        return records;
    }

    public void saveHistoryRecords(String name, int score){
        open();
        insertContact(name,score);
        close();
    }

    public boolean deleteHistoryRecords() {
        open();
        boolean deleteState = db.delete(DATABASE_TABLE, KEY_ROWID + "!=" + 0, null) > 0;
        close();
        return deleteState;
    }
}