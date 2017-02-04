package com.claudiodegio.dbsync;

import java.io.IOException;
import java.util.Map;

public interface DatabaseReader {

    int nextElement();

    Database readDatabase() throws IOException;

    Table readTable() throws IOException;

    Record readRecord(Map<String, ColumnMetadata> columnMetadataMap) throws IOException;

    void close();
}
