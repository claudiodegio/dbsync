package com.claudiodegio.dbsync.core;

import com.claudiodegio.dbsync.core.ColumnMetadata;
import com.claudiodegio.dbsync.core.Database;
import com.claudiodegio.dbsync.core.Record;
import com.claudiodegio.dbsync.core.Table;

import java.io.IOException;
import java.util.Map;

public interface DatabaseReader {

    int nextElement();

    Database readDatabase() throws IOException;

    Table readTable() throws IOException;

    Record readRecord(Map<String, ColumnMetadata> columnMetadataMap) throws IOException;

    void close();
}
