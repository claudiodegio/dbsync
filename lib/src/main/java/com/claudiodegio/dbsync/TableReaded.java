package com.claudiodegio.dbsync;


public class TableReaded {

    final private String mName;
    final private int mRecordCount;

    public TableReaded(String name, int recordCount) {
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
        return "TableReaded{" +
                "mName='" + mName + '\'' +
                ", mRecordCount=" + mRecordCount +
                '}';
    }
}
