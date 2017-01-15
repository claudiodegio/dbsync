package com.claudiodegio.dbsync.sample;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class TestReadWriteActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test_read_write);
        ButterKnife.bind(this);
    }


    @OnClick(R.id.btRead)
    public void goRead(){

        InputStream stream = getResources().openRawResource(R.raw.db_20170114_184802_3200_records);

        JsonDatabaseReader  jsonDatabaseReader = new JsonDatabaseReader();

        try {
            long duration = jsonDatabaseReader.readDatabase(stream);
            Toast.makeText(this, Long.toString(duration), Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();

            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    @OnClick(R.id.btWrite)
    public void goWrite(){

        File dir = getExternalFilesDir(null);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

        File file = new File(dir, "db_" + simpleDateFormat.format(new Date())+ ".json");

        JSonDatabaseWriter jSonDatabaseWriter = new JSonDatabaseWriter.Builder()
                .setDbName("db1_gen")
                .addTable("names_gen", 3000)
                .addTable("cities_gen", 100)
                .addTable("states_gen", 100).build();

        long duration = 0;
        try {
            duration = jSonDatabaseWriter.write(file);
            System.out.println("Duration: " + duration + " ms");
            System.out.println("Duration: " + duration/1000 + " s");
            Toast.makeText(this, "" + file.getAbsolutePath() + " duration: " + duration, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
