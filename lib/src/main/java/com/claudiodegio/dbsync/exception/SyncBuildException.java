package com.claudiodegio.dbsync.exception;


/**
 * Exeption throw on error during build procedure
 */
public class SyncBuildException extends RuntimeException {

    public SyncBuildException(String message) {
        super(message);
    }

}
