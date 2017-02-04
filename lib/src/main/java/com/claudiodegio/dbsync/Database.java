package com.claudiodegio.dbsync;

/**
 * Created by claud on 01/02/2017.
 */

public class Database {

    final private String mName;
    final private int mFormatVersion;
    final private int mTableCount;

    public Database(String name, int formatVersion, int tableCount) {
        this.mName = name;
        this.mFormatVersion = formatVersion;
        this.mTableCount = tableCount;
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


    @Override
    public String toString() {
        return "Database{" +
                "mName='" + mName + '\'' +
                ", mFormatVersion=" + mFormatVersion +
                ", mTableCount=" + mTableCount +
                '}';
    }

}
