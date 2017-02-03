package com.claudiodegio.dbsync;

import java.io.IOException;

public interface DatabaseWriter {

    void writeStartDatabase(String name, int numOfTable) throws IOException;
    void writeEndDatabase() throws IOException;

    void writeRecord(Record record) throws IOException;

    void writeStartTable(String name, int numOfRecord) throws IOException;
    void writeEndTable() throws IOException;

    void close() throws IOException;
}
