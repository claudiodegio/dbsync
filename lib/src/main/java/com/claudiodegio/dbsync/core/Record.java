package com.claudiodegio.dbsync.core;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

import java.util.ArrayList;


public class Record extends ArrayList<Value> {


    public Value findField(final String fieldName){

        Value value;

        value = IterableUtils.find(this, object -> object.getMetadata().getName().equals(fieldName));

        return value;
    }
}
