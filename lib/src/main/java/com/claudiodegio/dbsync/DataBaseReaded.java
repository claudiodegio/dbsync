package com.claudiodegio.dbsync;

/**
 * Created by claud on 01/02/2017.
 */

public class DatabaseReaded {

    final private String mName;
    final private int mFormatVersion;
    final private int mTableCount;

    public DatabaseReaded(String name, int formatVersion, int tableCount) {
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
        return "DatabaseReaded{" +
                "mName='" + mName + '\'' +
                ", mFormatVersion=" + mFormatVersion +
                ", mTableCount=" + mTableCount +
                '}';
    }

}
