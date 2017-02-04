package com.claudiodegio.dbsync;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlLiteUtility {


    public static Map<String, ColumnMetadata> readTableMetadataAsMap(final SQLiteDatabase db, final String tableName) {
        List<ColumnMetadata> columns;
        Map<String, ColumnMetadata> maps;

        columns = readTableMetadata(db, tableName);

        maps = new HashMap<>();

        for (ColumnMetadata column : columns) {
            maps.put(column.getName(), column);
        }

        return maps;
    }

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
                    type = ColumnMetadata.TYPE_LONG;
                    break;
                case "TEXT":
                    type = ColumnMetadata.TYPE_STRING;
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

    public static ColumnValue getCursorColumnValue(final Cursor cursor, final ColumnMetadata metadata) {
        String columnName;
        int index;
        long valueLong;
        String valueString;
        ColumnValue value;

        columnName = metadata.getName();
        index = cursor.getColumnIndex(columnName);

        if (cursor.isNull(index)) {
            value =  new ColumnValue(metadata);
        } else if(metadata.getType() == ColumnMetadata.TYPE_LONG) {
            valueLong = getCursorLong(cursor, columnName);
            value = new ColumnValue(valueLong, metadata);
        } else {
            valueString = getCursorString(cursor, columnName);
            value = new ColumnValue(valueString, metadata);
        }

        return value;
    }
}
