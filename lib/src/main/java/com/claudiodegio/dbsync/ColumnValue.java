package com.claudiodegio.dbsync;

public class ColumnValue {

    private String mValueString;
    private long mValueLong;
    private ColumnMetadata mMetadata;

    public ColumnValue(String valueString, ColumnMetadata metadata) {
        this.mValueString = valueString;
        this.mMetadata = metadata;
    }

    public ColumnValue(long valueLong, ColumnMetadata metadata) {
        this.mValueLong = valueLong;
        this.mMetadata = metadata;
    }

    public String getValueString() {
        return mValueString;
    }

    public long getValueLong() {
        return mValueLong;
    }

    public ColumnMetadata getMetadata() {
        return mMetadata;
    }

}
