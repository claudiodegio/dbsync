package com.claudiodegio.dbsync;

import com.claudiodegio.dbsync.core.JoinTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to describe a table to sync
 */
public class TableToSync {

    private String mName;
    final private List<String> mIgnoreColumns;
    final private String mCloudIdColumn;
    private String mIdColumn;
    private String mSendTimeColumn;
    private List<String> mMatchRules;
    final private String mFilter;

    final private List<JoinTable> mJoinTable;

    private TableToSync(final String name, final List<String> ignoreColumns, final String cloudIdColumn, final String idColumn, final String sendTimeColumn, final List<String> matchRules, final String filter, final  List<JoinTable> joinTable){
        this.mName = name;
        this.mIgnoreColumns = ignoreColumns;
        this.mCloudIdColumn = cloudIdColumn;
        this.mIdColumn = idColumn;
        this.mSendTimeColumn = sendTimeColumn;
        this.mMatchRules = matchRules;
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

    /**
     * Builder class
     */
    public static class Builder {

        final private String mName;
        final private List<String> mIgnoreColumns;
        private String mCloudIdColumn;
        private String mIdColumn;
        private String mSendTimeColumn;
        private List<String> mMatchRules;

        private String mFilter = "";

        final private List<JoinTable> mJoinTable = new ArrayList<>();

        public Builder(final String name){
            this.mName = name;
            this.mIgnoreColumns = new ArrayList<>(1);
            this.mIgnoreColumns.add("_id");
            this.mCloudIdColumn = "CLOUD_ID";
            this.mIdColumn = "_id";
            this.mSendTimeColumn = "SEND_TIME";
            this.mMatchRules = new ArrayList<>(1);
            this.mMatchRules.add("CLOUD_ID = :CLOUD_ID");
        }

        /**
         * Add a ignore column
         */
        public Builder addIgnoreColumns(final String ignoreColumn){
            this.mIgnoreColumns.add(ignoreColumn);
            return this;
        }

        /**
         * Set the cloud id column
         * @param cloudIdColumn the name o column
         */
        public Builder setCloudIdColumn(final String cloudIdColumn) {
            this.mCloudIdColumn = cloudIdColumn;
            return this;
        }

        /**
         * Set the id column
         * @param idColumn the name o column
         */
        public Builder setIdColumn(final String idColumn) {
            this.mIdColumn = idColumn;
            return this;
        }

        /**
         * Set the send time column
         * @param sendTimeColumn the name o column
         */
        public Builder setSendTimeColumn(final String sendTimeColumn) {
            this.mSendTimeColumn = sendTimeColumn;
            return this;
        }

        /**
         * Add a match rule
         * @param matchRule the rule to add
         */
        public Builder addMatchRule(final String matchRule) {
            mMatchRules.add(matchRule);
            return this;
        }

        /**
         * Set the filter in table
         * @param mFilter the filter to use
         */
        public Builder setFilter(String mFilter) {
            this.mFilter = mFilter;
            return this;
        }

        /**
         * Add a join table
         * @param refTable the reference table
         * @param joinColumn the join columns
ì         */
        public Builder addJoinTable(final TableToSync refTable, final String joinColumn) {
            mJoinTable.add(new JoinTable(refTable, joinColumn));
            return this;
        }

        /**
         * Build a new table to sync
         */
        public TableToSync build(){
            return new TableToSync(mName, mIgnoreColumns, mCloudIdColumn, mIdColumn, mSendTimeColumn, mMatchRules, mFilter, mJoinTable);
        }
    }

}
