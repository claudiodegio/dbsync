package com.claudiodegio.dbsync.core;

/**
 * Class to hold column value
 */
public class Value {

    private String mValueString;
    private long mValueLong;
    private ValueMetadata mMetadata;
    private boolean mNull = false;

    public Value(String valueString, ValueMetadata metadata) {
        this.mValueString = valueString;
        this.mMetadata = metadata;
        this.mNull = false;
    }

    public Value(long valueLong, ValueMetadata metadata) {
        this.mValueLong = valueLong;
        this.mMetadata = metadata;
        this.mNull = false;
    }

    public Value(ValueMetadata metadata) {
        this.mMetadata = metadata;
        this.mNull = true;
    }

    public String getValueString() {
        return mValueString;
    }

    public long getValueLong() {
        return mValueLong;
    }

    public ValueMetadata getMetadata() {
        return mMetadata;
    }

    public boolean isNull(){
        return mNull;
    }

    public String toSelectionArg(){
        switch (mMetadata.getType()) {
            case ValueMetadata.TYPE_LONG:
                return Long.toString(mValueLong);
            case ValueMetadata.TYPE_STRING:
                return mValueString;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        if (isNull()) {
            return "[NULL]";
        } else if (mMetadata.getType() == ValueMetadata.TYPE_LONG) {
            return Long.toString(mValueLong) + "L";
        } else {
            return mValueString;
        }
    }
}
