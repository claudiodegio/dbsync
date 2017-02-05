package com.claudiodegio.dbsync;

/**
 * Created by claud on 04/02/2017.
 */

public class RecordCounter {

    private int mRecordUpdated = 0;
    private int mRecordInserted = 0;

    public int getRecordInserted() {
        return mRecordInserted;
    }

    public int getRecordUpdated() {
        return mRecordUpdated;
    }

    public void incrementRecordUpdated(int numOfRecord){
        this.mRecordUpdated += numOfRecord;
    }

    public void incrementRecordInserted(int numOfRecord){
        this.mRecordInserted += numOfRecord;
    }

    public void incrementRecordUpdated(){
        this.mRecordUpdated += 1;
    }

    public void incrementRecordInserted(){
        this.mRecordInserted += 1;
    }
}
