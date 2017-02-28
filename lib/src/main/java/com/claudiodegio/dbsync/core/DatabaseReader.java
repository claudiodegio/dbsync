package com.claudiodegio.dbsync.core;

import java.io.IOException;
import java.util.Map;

public interface DatabaseReader {

    int nextElement();

    Database readDatabase() throws IOException;

    Table readTable() throws IOException;

    Record readRecord(Map<String, ValueMetadata> columnMetadataMap) throws IOException;

    void close();
}
