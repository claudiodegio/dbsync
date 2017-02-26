package com.claudiodegio.dbsync.core;


import com.claudiodegio.dbsync.TableToSync;

public class JoinTable {
    final private TableToSync mReferenceTable;
    final private String mJoinColumn;

    public JoinTable(final TableToSync table, String joinColumn) {
        this.mReferenceTable = table;
        this.mJoinColumn = joinColumn;
    }

    public String getJoinColumn() {
        return mJoinColumn;
    }

    public TableToSync getReferenceTable() {
        return mReferenceTable;
    }

}
