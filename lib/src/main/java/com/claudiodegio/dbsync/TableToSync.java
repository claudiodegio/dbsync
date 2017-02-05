package com.claudiodegio.dbsync;

import java.util.ArrayList;
import java.util.List;

public class TableToSync {

    private String mName;
    private List<String> mIgnoreColumns;
    private String mCloudIdColumn;
    private String mIdColumn;
    private String mDateCreatedColumn;
    private String mLastUpdatedColumn;
    private List<String> mMatchRules;

    private TableToSync(final String name){
        this.mName = name;
        this.mIgnoreColumns = new ArrayList<>(1);
        this.mIgnoreColumns.add("_id");
        this.mCloudIdColumn = "CLOUD_ID";
        this.mIdColumn = "_id";
        this.mDateCreatedColumn = "DATE_CREATED";
        this.mLastUpdatedColumn = "LAST_UPDATED";
        this.mMatchRules = new ArrayList<>(1);
        this.mMatchRules.add("CLOUD_ID = :CLOUD_ID");
    }

    public String getName() {
        return mName;
    }

    public boolean isColumnToIgnore(final String columnName) {
        return mIgnoreColumns.contains(columnName);
    }

    public String getCloudIdColumn() {
        return mCloudIdColumn;
    }

    public String getIdColumn() {
        return mIdColumn;
    }

    public String getDateCreatedColumn() {
        return mDateCreatedColumn;
    }

    public String getLastUpdatedColumn() {
        return mLastUpdatedColumn;
    }

    public List<String> getMatchRules(){
        return mMatchRules;
    }
    public static class Builder {

        final String mName;

        public Builder(final String name){
            this.mName = name;
        }

        public TableToSync build(){
            return new TableToSync(mName);
        }
    }

}
