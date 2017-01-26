package com.claudiodegio.dbsync;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class SqlLiteUtility {



    public static List<ColumnMetadata> readTableMetadata(final SQLiteDatabase db, final String tableName) {

        Cursor cursor = null;
        String columnName;
        String columnType;
        int notNull, pk, type;

        List<ColumnMetadata> list = new ArrayList<>();

        cursor = db.rawQuery("PRAGMA table_info('" + tableName + "')", null);

        while (cursor.moveToNext()){

            columnName = getCursorString(cursor, "name");
            columnType = getCursorString(cursor, "type");
            notNull = getCursorInt(cursor, "notnull");
            pk = getCursorInt(cursor, "pk");

            switch (columnType) {
                case "INTEGER":
                    type = ColumnMetadata.TYPE_INTEGER;
                    break;
                case "TEXT":
                    type = ColumnMetadata.TYPE_TEXT;
                    break;
                default:
                    throw new RuntimeException("Type " + columnType + " non supported !!!");
            }

           list.add(new ColumnMetadata(columnName, type, notNull == 1, pk == 1));
        }

        cursor.close();

        return list;
    }

    public static String getCursorString(final Cursor cursor, final String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return cursor.getString(index);
    }

    public static int getCursorInt(final Cursor cursor, final String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return cursor.getInt(index);
    }

    public static long getCursorLong(final Cursor cursor, final String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return cursor.getLong(index);
    }
}
