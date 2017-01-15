package com.claudiodegio.dbsync.sample;


import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JSonDatabaseWriter {

    Map<String, Integer> tablesToWrite;
    String databaseName;


    private JSonDatabaseWriter(String databaseName, Map<String, Integer> tablesToWrite){
        this.databaseName = databaseName;
        this.tablesToWrite = tablesToWrite;
    }

    long write(File file) throws IOException {

        long timestamp = System.currentTimeMillis();

        if (file.exists()) {
            file.delete();
        }

        JsonFactory f = new JsonFactory();
        JsonGenerator g = f.createGenerator(file, JsonEncoding.UTF8);
        g.setPrettyPrinter(new DefaultPrettyPrinter() );

        g.writeStartObject();

        g.writeFieldName("database");
        g.writeStartObject();
        // Nome database
        g.writeStringField("name",databaseName);

        // Tabelle
        g.writeFieldName("tables");
        g.writeStartArray();

        for (Map.Entry<String, Integer> entry : tablesToWrite.entrySet()) {

            g.writeStartObject();

            g.writeStringField("name",entry.getKey());

            g.writeArrayFieldStart("records");

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            for (int i = 0; i < entry.getValue();++i) {

                g.writeStartObject();

                g.writeNumberField("cId", new Date().getTime());

                String name = entry.getKey();
                name = name + " " + i;

                g.writeStringField("name", name);

                g.writeStringField("dateCreated", simpleDateFormat.format(new Date()));
                g.writeStringField("dateUpdated", simpleDateFormat.format(new Date()));


                g.writeEndObject();
            }

            g.writeEndArray();

            g.writeEndObject();
        }

        g.writeEndArray();
        g.writeEndObject();

        g.writeEndObject();
        g.close();

        return System.currentTimeMillis() - timestamp;
    }

    public static class Builder {

        String databaseName;
        Map<String, Integer> tablesToWrite = new HashMap<>();


        public Builder setDbName(String name) {
            this.databaseName = name;
            return this;
        }

        public Builder addTable(String name, int recordCount) {
            tablesToWrite.put(name, recordCount);
            return this;
        }

        public JSonDatabaseWriter build(){
            return new JSonDatabaseWriter(databaseName, tablesToWrite);
        }

    }
}
