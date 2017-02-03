package com.claudiodegio.dbsync;


import java.io.File;
import java.io.InputStream;

public interface CloudProvider {


    void uploadFile(File tempFile);

    InputStream downloadFile();
}
