package com.claudiodegio.dbsync;

// Potrei farlo con il decorator
public class ColumnValue {

    private String mValueString;
    private long mValueLong;
    private ColumnMetadata mMetadata;
    private boolean mNull = false;

    public ColumnValue(String valueString, ColumnMetadata metadata) {
        this.mValueString = valueString;
        this.mMetadata = metadata;
        this.mNull = false;
    }

    public ColumnValue(long valueLong, ColumnMetadata metadata) {
        this.mValueLong = valueLong;
        this.mMetadata = metadata;
        this.mNull = false;
    }

    public ColumnValue(ColumnMetadata metadata) {
        this.mMetadata = metadata;
        this.mNull = true;
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

    public boolean isNull(){
        return mNull;
    }

    @Override
    public String toString() {
        if (isNull()) {
            return "[NULL]";
        } else if (mMetadata.getType() == ColumnMetadata.TYPE_LONG) {
            return Long.toString(mValueLong) + "L";
        } else {
            return mValueString;
        }
    }
}
