package com.claudiodegio.dbsync;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public interface DatabaseReader {

    int nextElement();

    DatabaseReaded readDatabase() throws IOException;

    TableReaded readTable() throws IOException;

    Record readRecord(Map<String, ColumnMetadata> columnMetadataMap) throws IOException;
}
