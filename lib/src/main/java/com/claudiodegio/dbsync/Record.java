package com.claudiodegio.dbsync;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.functors.EqualPredicate;

import java.util.ArrayList;


public class Record extends ArrayList<ColumnValue> {


    public ColumnValue findField(final String fieldName){

        ColumnValue value;

        value = IterableUtils.find(this, new Predicate<ColumnValue>() {
            @Override
            public boolean evaluate(ColumnValue object) {
                return object.getMetadata().getName().equals(fieldName);
            }
        });

        return value;
    }
}
