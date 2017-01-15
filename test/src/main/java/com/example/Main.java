package com.example;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static com.example.JSonDatabaseWriter.*;

public class Main {

    public static void main(String[] args) {

        File wdFile = new File(".");
        System.out.println(" - Inizio -");
        System.out.println("WD:" + wdFile.getAbsolutePath());

        testWrite();


        System.out.println(" - Fine -");

    }

    static void testRead(){

        JsonDatabaseReader jsonDatabaseReader = new JsonDatabaseReader();

        try {
            long duration = jsonDatabaseReader.readDatabase(new File("D:\\git\\easy-mobile\\DbSyncSample\\test\\src\\main\\groovy\\com\\example\\db_20170114_002331_9_records.json"));

            System.out.println("Duration: " + duration + " ms");
            System.out.println("Duration: " + duration/1000 + " s");

            duration = jsonDatabaseReader.readDatabase(new File("D:\\git\\easy-mobile\\DbSyncSample\\test\\src\\main\\groovy\\com\\example\\db_20170114_184802_3200_records.json"));

            System.out.println("Duration: " + duration + " ms");
            System.out.println("Duration: " + duration/1000 + " s");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void testWrite(){

        JSonDatabaseWriter jSonDatabaseWriter = new JSonDatabaseWriter.Builder()
                .setDbName("db1_gen")
                .addTable("names_gen", 3000)
                .addTable("cities_gen", 100)
                .addTable("states_gen", 100).build();


        try {
            long duration = jSonDatabaseWriter.write(new File("D:\\git\\easy-mobile\\DbSyncSample\\db_gen.json"));
            System.out.println("Duration: " + duration + " ms");
            System.out.println("Duration: " + duration/1000 + " s");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
