package com.claudiodegio.dbsync.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by claud on 10/03/2018.
 */

public class RecordChanged {


    private List<Long> mInseredId = new ArrayList<>();
    private List<Long> mUpdatedId = new ArrayList<>();

    private int mRecordUpdated = 0;
    private int mRecordInserted = 0;


    void addInseredId(Long id) {
        mInseredId.add(id);
    }

    void addUpdatedId(Long id) {
        mUpdatedId.add(id);
    }

    public List<Long> getInseredId() {
        return mInseredId;
    }

    public List<Long> getUpdatedId() {
        return mUpdatedId;
    }

    public int getRecordInserted() {
        return mRecordInserted;
    }

    public int getRecordUpdated() {
        return mRecordUpdated;
    }

    void incrementRecordUpdated(){
        this.mRecordUpdated += 1;
    }

    void incrementRecordInserted(){
        this.mRecordInserted += 1;
    }
}
