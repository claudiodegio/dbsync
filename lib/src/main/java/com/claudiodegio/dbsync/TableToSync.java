package com.claudiodegio.dbsync;

import com.claudiodegio.dbsync.core.JoinTable;

import java.util.ArrayList;
import java.util.List;

public class TableToSync {

    private String mName;
    private List<String> mIgnoreColumns;
    private String mCloudIdColumn;
    private String mIdColumn;
    private String mSendTimeColumn;
    private List<String> mMatchRules;
    final private String mFilter;

    final private List<JoinTable> mJoinTable;

    private TableToSync(final String name, final String filter, final  List<JoinTable> joinTable){
        this.mName = name;
        this.mIgnoreColumns = new ArrayList<>(1);
        this.mIgnoreColumns.add("_id");
        this.mCloudIdColumn = "CLOUD_ID";
        this.mIdColumn = "_id";
        this.mSendTimeColumn = "SEND_TIME";
        this.mMatchRules = new ArrayList<>(1);
        this.mMatchRules.add("CLOUD_ID = :CLOUD_ID");
        this.mFilter = filter;
        this.mJoinTable = joinTable;
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

    public String getSendTimeColumn() {
        return mSendTimeColumn;
    }

    public List<String> getMatchRules(){
        return mMatchRules;
    }

    public String getFilter() {return mFilter; }

    public List<JoinTable> getJoinTable() {
        return mJoinTable;
    }

    public boolean hasJoinTable(){
        return !mJoinTable.isEmpty();
    }

    public static class Builder {

        final private String mName;
        private String mFilter = "";
        final private List<JoinTable> mJoinTable = new ArrayList<>();

        public Builder(final String name){
            this.mName = name;
        }

        public Builder setFilter(String mFilter) {
            this.mFilter = mFilter;
            return this;
        }

        public Builder addJoinTable(final TableToSync refTable, final String joinColumn) {
            mJoinTable.add(new JoinTable(refTable, joinColumn));
            return this;
        }

        public TableToSync build(){
            return new TableToSync(mName, mFilter, mJoinTable);
        }
    }

}
