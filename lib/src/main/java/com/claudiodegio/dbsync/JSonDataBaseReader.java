package com.claudiodegio.dbsync;

import android.text.TextUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class JSonDatabaseReader implements DatabaseReader {


    // TODO metterlo in qualche classe
    final static public int START_DB = 1;
    final static public int START_TABLE = 2;
    final static public int RECORD = 3;
    final static public int END = 4;

    private State mState;

    private JsonParser mJp;

    private Database mDatabase;
    private Table mCurrentTable;
    private Record mCurrentRecord;

    public JSonDatabaseReader(InputStream inputStream) throws IOException {
        JsonFactory jsonFactory;

        jsonFactory = new JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory
        mJp = jsonFactory.createParser(inputStream); // or URL, Stream, Reader, String, byte[]
        mState = new DatabaseState();
    }

    @Override
    public int nextElement() {
        return mState.getElementType();
    }

    @Override
    public Database readDatabase() throws IOException {

        if (mState.getElementType() != START_DB) {
            throw new IOException("Unable to read wrong current element type " + mState.getElementType());
        }

        mState.handle();

        return mDatabase;
    }

    @Override
    public Table readTable() throws IOException {
        if (mState.getElementType() != START_TABLE) {
            throw new IOException("Unable to read wrong current element type " + mState.getElementType());
        }

        mState.handle();

        return mCurrentTable;
    }

    @Override
    public Record readRecord(final Map<String, ColumnMetadata> colMetadataMap) throws IOException {
        if (mState.getElementType() != RECORD) {
            throw new IOException("Unable to read wrong current element type " + mState.getElementType());
        }

        if (colMetadataMap == null) {
            throw new RuntimeException("columnMetadataMap cannot be null");
        }

        if (colMetadataMap.isEmpty()) {
            throw new RuntimeException("columnMetadataMap cannot be empty");
        }

        ((RecordTableState)mState).handle(colMetadataMap);

        return mCurrentRecord;
    }

    @Override
    public void close() {
        if (!mJp.isClosed()){
            IOUtils.closeQuietly(mJp);
        }
    }

    private String readNextTokenAsString() throws IOException {
        if (mJp.nextToken() != JsonToken.VALUE_STRING) {
            if (mJp.getCurrentToken() == JsonToken.VALUE_NULL) {
                throw new IOException("Unable to parse token as STRING is null line:" + mJp.getCurrentLocation().getLineNr());
            } else {
                throw new IOException("Unable to parse token as STRING line:" + mJp.getCurrentLocation().getLineNr());
            }
        }
        return mJp.getValueAsString();
    }

    private Integer readNextTokenAsInt() throws IOException {
        if (mJp.nextToken() != JsonToken.VALUE_NUMBER_INT) {
            if (mJp.getCurrentToken() == JsonToken.VALUE_NULL) {
                throw new IOException("Unable to parse token as NUMBER_INT is null line:" + mJp.getCurrentLocation().getLineNr());
            } else {
                throw new IOException("Unable to parse token as NUMBER_INT line:" + mJp.getCurrentLocation().getLineNr());
            }
        }
        return mJp.getIntValue();
    }


    private abstract class State {
        abstract int getElementType();
        abstract void handle()  throws IOException;
    }

    private class DatabaseState extends State {

        @Override
        int getElementType() {
            return START_DB;
        }

        @Override
        void handle() throws IOException {
            String fieldName;
            String databaseName = null;
            int tableCount = -1;
            int formatVersion = -1;
            boolean foundTable = false;

            // The first token is {
            if(mJp.nextToken() != JsonToken.START_OBJECT){
                throw new IOException("Unable to read start object of db element line:" + mJp.getCurrentLocation().getLineNr());
            }

            // Read the info of database
            while (mJp.nextToken() == JsonToken.FIELD_NAME) {
                fieldName = mJp.getCurrentName();

                if (fieldName.equals("name")) {
                    databaseName = readNextTokenAsString();
                } else if (fieldName.equals("tableCount")){
                    tableCount = readNextTokenAsInt();
                } else if (fieldName.equals("formatVersion")){
                   formatVersion =  readNextTokenAsInt();
                }  else if (fieldName.equals("tables")){
                   foundTable = true;
                    break;
                } else {
                    throw new IOException("Unable to parse found token:"+ fieldName + " line:" + mJp.getCurrentLocation().getLineNr());
                }
            }

            if (TextUtils.isEmpty(databaseName)) {
                throw new IOException("Unable to read database name field empty");
            }

            if (tableCount == -1) {
                throw new IOException("Unable to read table count");
            }

            if (formatVersion == -1) {
                throw new IOException("Unable to read format version");
            }

            // Il sezione delle tabelle deve essere l'ultimo della sezione
            if (!foundTable || !mJp.getCurrentName().equals("tables")) {
                throw new IOException("Unable to read tables section");
            }

            mDatabase = new Database(databaseName, formatVersion, tableCount);

            // The first token is [
            if(mJp.nextToken() != JsonToken.START_ARRAY){
                throw new IOException("Unable to read start array of of tables line:" + mJp.getCurrentLocation().getLineNr());
            }

            if(mJp.nextToken() != JsonToken.START_OBJECT){
                throw new IOException("Unable to read start object of table element line:" + mJp.getCurrentLocation().getLineNr());
            }

            // passo allo stato successivo (inizio di una tabella)
            mState = new StartTableState();
        }
    }

    private class StartTableState extends State {

        @Override
        int getElementType() {
            return START_TABLE;
        }

        @Override
        void handle() throws IOException {
            String fieldName;
            int recordsCount = -1;
            String tableName = "";
            boolean foundRecord = false;

            // Read the first part of table
            if(mJp.getCurrentToken() != JsonToken.START_OBJECT){
                throw new IOException("Unable to read start table object line:" + mJp.getCurrentLocation().getLineNr());
            }

            while (mJp.nextToken() == JsonToken.FIELD_NAME) {
                fieldName = mJp.getCurrentName();

                if (fieldName.equals("name")) {
                    tableName = readNextTokenAsString();
                } else if (fieldName.equals("recordsCount")) {
                    recordsCount = readNextTokenAsInt();
                } else if (fieldName.equals("records")) {
                    foundRecord = true;
                    break;
                }
            }

            if (TextUtils.isEmpty(tableName)) {
                throw new IOException("Unable to read table name");
            }

            if (recordsCount == -1) {
                throw new IOException("Unable to read table recordsCount");
            }

            if (!foundRecord || !mJp.getCurrentName().equals("records")) {
                throw new IOException("Unable to read table records fields");
            }

            // the first token of record is open array
            if(mJp.nextToken() != JsonToken.START_ARRAY){
                throw new IOException("Unable to read start array of of records line:" + mJp.getCurrentLocation().getLineNr());
            }

            mCurrentTable = new Table(tableName, recordsCount);
            mState = new RecordTableState();
        }
    }

    private class RecordTableState extends State {

        @Override
        int getElementType() {
            return RECORD;
        }

        @Override
        void handle() throws IOException {
        }

        void handle(final Map<String, ColumnMetadata> colMetadataMap) throws IOException {
            String fieldName;
            ColumnMetadata columnMetadata;
            JSonConverter converter;
            ColumnValue value;

            // Creare a new record
            mCurrentRecord = new Record();
            while (mJp.nextToken() != JsonToken.END_OBJECT) {

                if (mJp.getCurrentToken() == JsonToken.FIELD_NAME) {
                    fieldName = mJp.getCurrentName();

                    // Get column metadata
                    if (!colMetadataMap.containsKey(fieldName)){
                        throw new IOException("Unable to find columns metadata for columns " + fieldName);
                    }

                    columnMetadata = colMetadataMap.get(fieldName);

                    // Build the data converter and convert
                    converter = JSonConverterFactory.buildConverter(columnMetadata);
                    value = converter.jsonToColumnValue(mJp, columnMetadata);

                    mCurrentRecord.add(value);
                }

            }

            // Go to next token
            mJp.nextToken();
            if (mJp.getCurrentToken() == JsonToken.START_OBJECT) {
                // The current token is start of object -> new record no change state required
                return;
            } else if (mJp.getCurrentToken() == JsonToken.END_ARRAY) {
                // The current token is end of array -> end of record, end of table, end of database

                // Consume end of object of current table
                mJp.nextToken();
                if(mJp.getCurrentToken() != JsonToken.END_OBJECT){
                    throw new IOException("Unable to read end of table expected end of object line :" + mJp.getCurrentLocation().getLineNr());
                }

                // Consume other token to detec end of database
                // if START_OBJECT -> new TableToSync
                // if END_ARRAY -> end of database
                mJp.nextToken();
                if (mJp.getCurrentToken() == JsonToken.START_OBJECT) {
                    mState = new StartTableState();
                } else if (mJp.getCurrentToken() == JsonToken.END_ARRAY) {
                    mState = new EndState();
                } else {
                    throw new IOException("Unable to read start of table expected { or ] òine:" + mJp.getCurrentLocation().getLineNr());
                }
            } else {
                throw new IOException("Unexpected token " + mJp.getCurrentToken() + " line:" + mJp.getCurrentLocation().getLineNr());
            }
        }
    }

    private class EndState extends State {

        @Override
        int getElementType() {
            return END;
        }

        @Override
        void handle() throws IOException {

        }
    }
}
