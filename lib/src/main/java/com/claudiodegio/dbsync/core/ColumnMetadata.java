package com.claudiodegio.dbsync.core;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ColumnMetadata {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_LONG, TYPE_STRING})
    public @interface Type {}
    static final public int TYPE_LONG = 0;
    static final public int TYPE_STRING = 1;

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

    public ColumnMetadata(String name, int type) {
        this(name, type, false, false);
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
