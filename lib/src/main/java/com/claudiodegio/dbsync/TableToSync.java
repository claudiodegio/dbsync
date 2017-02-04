package com.claudiodegio.dbsync;

import java.util.ArrayList;
import java.util.List;

public class TableToSync {

    private String mName;
    private List<String> mIgnoreColumns;
    private String mCloudIdColumn;
    private String mIdColumn;


    private TableToSync(final String name){
        this.mName = name;
        this.mIgnoreColumns = new ArrayList<>();
        this.mIgnoreColumns.add("_id");
        this.mCloudIdColumn = "CLOUD_ID";
        this.mIdColumn = "_id";
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
