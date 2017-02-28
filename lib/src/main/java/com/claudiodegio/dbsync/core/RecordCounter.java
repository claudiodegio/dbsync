package com.claudiodegio.dbsync.core;


public class RecordCounter {

    private int mRecordUpdated = 0;
    private int mRecordInserted = 0;

    public int getRecordInserted() {
        return mRecordInserted;
    }

    public int getRecordUpdated() {
        return mRecordUpdated;
    }

    public void incrementRecordUpdated(){
        this.mRecordUpdated += 1;
    }

    public void incrementRecordInserted(){
        this.mRecordInserted += 1;
    }
}
