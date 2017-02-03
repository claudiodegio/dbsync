package com.claudiodegio.dbsync;

// Potrei farlo con il decorator
public class ColumnValue {

    private String mValueString;
    private Long mValueLong;
    private ColumnMetadata mMetadata;

    public ColumnValue(String valueString, ColumnMetadata metadata) {
        this.mValueString = valueString;
        this.mMetadata = metadata;
    }

    public ColumnValue(Long valueLong, ColumnMetadata metadata) {
        this.mValueLong = valueLong;
        this.mMetadata = metadata;
    }

    public ColumnValue(ColumnMetadata metadata) {
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

    @Override
    public String toString() {
        if (mValueLong != null)
            return mValueLong.toString();
        else
            return mValueString;
    }
}
