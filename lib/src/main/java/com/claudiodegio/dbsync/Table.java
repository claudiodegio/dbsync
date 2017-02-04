package com.claudiodegio.dbsync;


public class Table {

    final private String mName;
    final private int mRecordCount;

    public Table(String name, int recordCount) {
        this.mName = name;
        this.mRecordCount = recordCount;
    }

    public int getRecordCount() {
        return mRecordCount;
    }

    public String getName() {
        return mName;
    }

    @Override
    public String toString() {
        return "Table{" +
                "mName='" + mName + '\'' +
                ", mRecordCount=" + mRecordCount +
                '}';
    }
}
