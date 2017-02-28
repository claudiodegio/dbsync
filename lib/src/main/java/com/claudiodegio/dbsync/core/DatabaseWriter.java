package com.claudiodegio.dbsync.core;

import java.io.IOException;

public interface DatabaseWriter {

    void writeDatabase(String name, int numOfTable, int schemaVersion) throws IOException;

    void writeTable(String name, int numOfRecord) throws IOException;

    void writeRecord(Record record) throws IOException;

    void close() throws IOException;
}
