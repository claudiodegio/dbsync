package com.example;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;


public class JsonDatabaseReader {


    public Map<String, Integer> numeriche;


    long readDatabase(File file) throws IOException {
        long timestamp = 0;

        numeriche = new HashMap<>();

        timestamp = System.currentTimeMillis();
        JsonFactory jsonFactory = new JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory
        JsonParser jp = jsonFactory.createParser(file); // or URL, Stream, Reader, String, byte[]

        jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)

        // passo a leggere il database
        String fieldName;
        if (jp.nextToken() == JsonToken.FIELD_NAME) {
            fieldName = jp.getCurrentName();
            jp.nextToken(); // START_OBJECT INIZIO di un nuovo oggetto database

            // Passo al campo nome
            while (jp.nextToken() == JsonToken.FIELD_NAME) {
                fieldName = jp.getCurrentName();
                jp.nextToken(); // Vado al valore
                if ("name".equals(fieldName)) {
                    System.out.println("Database Name:" + jp.getValueAsString());
                } else if ("tables".equals(fieldName)) {
                    while (jp.nextToken() != JsonToken.END_ARRAY) {
                        readTable(jp);
                    }
                }
            }
        }

        jp.close();

        long duration = System.currentTimeMillis() - timestamp;

        return duration;
    }


    private void readTable(JsonParser jp) throws IOException {
        JsonToken token;
        String tableName = "";
        int recordCount = 0;

        while((token = jp.nextToken()) != JsonToken.END_OBJECT) {

            if (token == JsonToken.FIELD_NAME) {
                if ("name".equals(jp.getCurrentName())) {
                    tableName = jp.nextTextValue();
                } else if ("records".equals(jp.getCurrentName())){
                    // Ciclo la lista dei records
                    while((token = jp.nextToken()) != JsonToken.END_ARRAY) {
                        readRecord(jp);
                        recordCount++;
                    }
                }
            }
        }
        System.out.println("\tTable: " + tableName + " records:" + recordCount);

        numeriche.put(tableName, recordCount);
    }

    Map<String, Object> readRecord(JsonParser jp) throws IOException {

        Map<String, Object> record = new HashMap<>();
        String fielName =  "";
        Object value;
        JsonToken token;

        while((token = jp.nextToken()) != JsonToken.END_OBJECT) {
            if (token == JsonToken.FIELD_NAME) {
                fielName = jp.getCurrentName();
            } else if (token == JsonToken.VALUE_STRING) {
                value = jp.getValueAsString();
                record.put(fielName, value);
            } else if (token == JsonToken.VALUE_NUMBER_INT) {
                value = jp.getValueAsLong();
                record.put(fielName, value);
            }
        }

        //System.out.println(record.toString());
        return record;
    }
}
