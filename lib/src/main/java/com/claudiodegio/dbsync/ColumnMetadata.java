package com.claudiodegio.dbsync;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ColumnMetadata {


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_INTEGER, TYPE_TEXT})
    public @interface Type {}
    static final int TYPE_INTEGER = 0;
    static final int TYPE_TEXT = 1;

    private String name;
    private @Type int type;
    private boolean notNull;
    private boolean pk;

    public ColumnMetadata(String name, int type, boolean notNull, boolean pk) {
        this.name = name;
        this.type = type;
        this.notNull = notNull;
        this.pk = pk;
    }

    public boolean isPk() {
        return pk;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ColumnMetadata{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", notNull=" + notNull +
                ", pk=" + pk +
                '}';
    }
}
