package com.claudiodegio.dbsync.core;

public class Database {

    final private String mName;
    final private int mFormatVersion;
    final private int mTableCount;
    final private int mSchemaVersion;


    public Database(String name, int formatVersion, int tableCount, int schemaVersion) {
        this.mName = name;
        this.mFormatVersion = formatVersion;
        this.mTableCount = tableCount;
        this.mSchemaVersion = schemaVersion;
    }
    public int getTableCount() {
        return mTableCount;
    }

    public int getFormatVersion() {
        return mFormatVersion;
    }

    public String getName() {
        return mName;
    }

    public int getSchemaVersion() {
        return mSchemaVersion;
    }

    @Override
    public String toString() {
        return "Database{" +
                "mName='" + mName + '\'' +
                ", mFormatVersion=" + mFormatVersion +
                ", mSchemaVersion=" + mSchemaVersion +
                ", mTableCount=" + mTableCount +
                '}';
    }

}
