package com.claudiodegio.dbsync;


import java.io.File;

public interface CloudProvider {


    void uploadFile(File tempFile);
}
