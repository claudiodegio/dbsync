package com.claudiodegio.dbsync;


import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;
import java.io.OutputStream;

public class JSonDatabaseWriter implements DatabaseWriter {

    private final static String TAG = "JSonDatabaseWriter";
    private final OutputStream mOutStream;
    private final JsonGenerator mGen;

    public JSonDatabaseWriter(final OutputStream outStream) throws IOException {
        this.mOutStream = outStream;

        JsonFactory f = new JsonFactory();
        mGen = f.createGenerator(outStream, JsonEncoding.UTF8);
        mGen.setPrettyPrinter(new DefaultPrettyPrinter());
    }

    @Override
    public void writeStartDatabase(String name, int numOfTable) throws IOException {
        mGen.writeStartObject();
        mGen.writeStringField("name", name);
        mGen.writeNumberField("formatVersion", 1);
        mGen.writeNumberField("tableCount", numOfTable);
        mGen.writeFieldName("tables");
        mGen.writeStartArray();
    }

    @Override
    public void writeEndDatabase() throws IOException {
        mGen.writeEndArray();
        mGen.writeEndObject();
    }

    @Override
    public void writeRecord(Record record) throws IOException {
        String fieldName;
        mGen.writeStartObject();
        for (ColumnValue value : record) {

            fieldName = value.getMetadata().getName();

            switch (value.getMetadata().getType()) {
                case ColumnMetadata.TYPE_LONG:
                    mGen.writeNumberField(fieldName, value.getValueLong());
                    break;
                case ColumnMetadata.TYPE_STRING:
                    mGen.writeStringField(fieldName, value.getValueString());
                    break;
            }
        }
        mGen.writeEndObject();
    }

    @Override
    public void writeStartTable(String name, int numOfRecord) throws IOException {
        mGen.writeStartObject();
        mGen.writeStringField("name", name);
        mGen.writeNumberField("recordsCount", numOfRecord);
        mGen.writeFieldName("records");
        mGen.writeStartArray();
    }

    @Override
    public void writeEndTable() throws IOException {
        mGen.writeEndArray(); // records
        mGen.writeEndObject();
    }

    @Override
    public void close() throws IOException {
        mGen.flush();
        mGen.close();
        mOutStream.close();
    }
}
