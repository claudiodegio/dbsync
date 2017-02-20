package com.claudiodegio.dbsync.sample;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Created by claud on 20/02/2017.
 */

public class Utility {


    final static String formatDateTimeNoTimeZone(Date date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        return dateFormat.format(date);
    }
}
