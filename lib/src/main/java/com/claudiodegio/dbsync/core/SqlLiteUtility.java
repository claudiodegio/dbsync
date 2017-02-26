package com.claudiodegio.dbsync.core;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static ColumnValue getCursorColumnValue(final Cursor cursor, final String columnName, @ColumnMetadata.Type int type) {
        return SqlLiteUtility.getCursorColumnValue(cursor,  new ColumnMetadata(columnName, type));
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

    public static void columnValueToContentValues(final ColumnValue value, final ContentValues contentValues) {
        ColumnMetadata metadata;
        String fieldName;

        fieldName = value.getMetadata().getName();

        if (value.isNull()) {
            contentValues.putNull(fieldName);
            return;
        }

        metadata = value.getMetadata();

        switch (metadata.getType()) {
            case ColumnMetadata.TYPE_LONG:
                contentValues.put(fieldName, value.getValueLong());
                break;
            case ColumnMetadata.TYPE_STRING:
                contentValues.put(fieldName, value.getValueString());
                break;
        }
    }

    static SqlWithBinding sqlWithMapToSqlWithBinding(final String sql){
        String sqlWithBind;
        List<String> selectionArg;
        Pattern p;
        Matcher m;

        // Find the binding params
        p = Pattern.compile(":([\\w]+)");
        m = p.matcher(sql);

        selectionArg = new ArrayList<>();
        while (m.find()) {
            selectionArg.add(m.group(1));
        }

        sqlWithBind = sql.replaceAll(":([\\w]+)", "?");

        return new SqlWithBinding(sql, sqlWithBind, selectionArg);
    }

    static public class SqlWithBinding {
        private String original;
        private String parsed;
        private List<String> selectionArgs;

        private SqlWithBinding(String original, String parsed, List<String> selectionArgs) {
            this.original = original;
            this.parsed = parsed;
            this.selectionArgs = selectionArgs;
        }

        public String getParsed() {
            return parsed;
        }

        public String getOriginal() {
            return original;
        }

        public String [] getArgs(){
            return selectionArgs.toArray( new String[selectionArgs.size()]);
        }

        @Override
        public String toString() {
            return "SqlWithBinding{" +
                    "original='" + original + '\'' +
                    ", parsed='" + parsed + '\'' +
                    ", selectionArgs=" + selectionArgs +
                    '}';
        }
    }
}
