package com.claudiodegio.dbsync.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;


import com.claudiodegio.dbsync.sample.db1.MainDb1Activity;
import com.claudiodegio.dbsync.sample.db2.MainDb2Activity;
import com.claudiodegio.dbsync.sample.db3.MainDb3Activity;
import com.claudiodegio.dbsync.sample.db4.MainDb4Activity;
import com.claudiodegio.dbsync.sample.db5.MainDb5Activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.ButterKnife;
import butterknife.OnClick;
import im.dino.dbinspector.activities.DbInspectorActivity;

public class MainActivity extends AppCompatActivity {

    static final private Logger log = LoggerFactory.getLogger(MainActivity.class);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @OnClick(R.id.btToDbManager)
    public void goToDBManager(){

        startActivity(new Intent(this, DbInspectorActivity.class));
    }

    @OnClick(R.id.btDb1)
    public void goToDB1(){
        startActivity(new Intent(this, MainDb1Activity.class));
    }

    @OnClick(R.id.btDb2)
    public void goToDB2(){
        startActivity(new Intent(this, MainDb2Activity.class));
    }

    @OnClick(R.id.btDb3)
    public void goToDB3(){
        startActivity(new Intent(this, MainDb3Activity.class));
    }


    @OnClick(R.id.btDb4)
    public void goToDB4(){
        startActivity(new Intent(this, MainDb4Activity.class));
    }

    @OnClick(R.id.btDb5)
    public void goToDB5(){
        startActivity(new Intent(this, MainDb5Activity.class));
    }
}
