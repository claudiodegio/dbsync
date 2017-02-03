package com.claudiodegio.dbsync;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DBSync {

    final static private String TAG = "DBSync";

    final private CloudProvider mCloudProvider;
    final private SQLiteDatabase mDB;
    final private List<Table> mTables;
    final private Context mCtx;
    final private String mDataBaseName;

    private DBSync(final Context ctx, final CloudProvider cloudProvider, final SQLiteDatabase db, final String dataBaseName, final List<Table> tables){
        this.mCtx = ctx;
        this.mCloudProvider = cloudProvider;
        this.mDB = db;
        this.mTables = tables;
        this.mDataBaseName = dataBaseName;
    }

    // TODO fare la versione sincrona e async
    public SyncResult sync() {
        File tempFbFile = null;
        InputStream inputStream = null;

        try {
            // upload
            inputStream = mCloudProvider.downloadFile();

            readDatabase(inputStream);

        /*    // populateUUID
            populateUUID();

            // Write the database file
            tempFbFile = writeDateBaseFile();

            // Upload file to cloud
            mCloudProvider.uploadFile(tempFbFile);*/

            // ALL OK
            return new SyncResult(new SyncStatus(SyncStatus.OK));
        } catch (SyncException e) {
            return new SyncResult(e.getStatus());
        } catch (Exception e) {
            // TODO capire se lanciare un eccezione o ritorno con errore
            return new SyncResult(new SyncStatus(SyncStatus.ERROR, e.getMessage()));
        } finally {

            IOUtils.closeQuietly(inputStream);
            if (tempFbFile != null && tempFbFile.exists()) {
                Log.d(TAG, "delete db temp file:" + tempFbFile.getName());
            }
        }
    }


    private File writeDateBaseFile() throws SyncException {
        File tempDbFile;
        FileOutputStream outStream;
        DatabaseWriter writer;

        try {
            tempDbFile = File.createTempFile("database", ".json");

            Log.i(TAG, "Create tmp db file: " + tempDbFile.getAbsolutePath());

            // Open temp file
            outStream = new FileOutputStream(tempDbFile);
            writer = new JSonDatabaseWriter(outStream);

            // Write database start
            writer.writeStartDatabase(mDataBaseName, mTables.size());

            // Write tables
            for (Table table : mTables) {
                serializeTable(table, writer);
            }

            // Close database
            writer.writeEndDatabase();
            writer.close();

            Log.i(TAG, "Created DB file with size: " + tempDbFile.length());
            return tempDbFile;
        } catch (Exception e) {
            throw new SyncException(SyncStatus.ERROR_WRITING_TMP_DB, e.getMessage());
        }
    }

    private void serializeTable(final Table table, final DatabaseWriter writer) throws IOException{
        List<ColumnMetadata> columnsMetadata;
        Cursor cur;
        ColumnValue value = null;
        Record record;

        columnsMetadata = SqlLiteUtility.readTableMetadata(mDB, table.getName());

        // TODO Gestire la close pulita
        cur = mDB.query(table.getName(), null, null, null, null, null, null);

        Log.i(TAG, "Write table:" + table.getName()+ " records:" + cur.getCount());

        writer.writeStartTable(table.getName(), cur.getCount());

        while (cur.moveToNext()) {
            record = new Record();

            for (ColumnMetadata colMeta : columnsMetadata) {
                if (!table.isColumnToIgnore(colMeta.getName())){

                    // valore, nome, typo dato
                    switch (colMeta.getType()) {
                        case ColumnMetadata.TYPE_LONG:
                            value = new ColumnValue(SqlLiteUtility.getCursorLong(cur, colMeta.getName()), colMeta);
                            break;

                        case ColumnMetadata.TYPE_STRING:
                             value = new ColumnValue(SqlLiteUtility.getCursorString(cur, colMeta.getName()), colMeta);
                            break;
                    }

                    record.add(value);
                }
            }

            writer.writeRecord(record);
        }

        cur.close();
        writer.writeEndTable();
    }

    private void populateUUID(){

        // populate UUID Table
        for (Table table : mTables) {
            populateUUID(table);
        }
    }

    private void populateUUID(Table table){
        String selection;
        Cursor cur = null;
        String uuid;
        int id;
        int rowCount;
        ContentValues contentValuesUpdate;
        Log.i(TAG, "start populateUUID for table:" +table.getName()+ " idColumn:" + table.getIdColumn() + " CloudIdColumn:" + table.getCloudIdColumn());

        selection = table.getCloudIdColumn() + " IS NULL OR " + table.getCloudIdColumn() + " = \"\"";

        try {
            cur = mDB.query(table.getName(), new String[]{table.getIdColumn()}, selection, null, null, null, null);

            rowCount = 0;
            contentValuesUpdate = new ContentValues();

            // TODO check uuid it'unique
            while(cur.moveToNext()) {
                id = cur.getInt(0);
                uuid = UUID.randomUUID().toString();

                Log.d(TAG, "assing uuid:" + uuid + " to id:" + id);
                // Update of cloud id
                contentValuesUpdate.put(table.getCloudIdColumn(), uuid);

                mDB.update(table.getName(), contentValuesUpdate, table.getIdColumn() + " = ?", new String[]{Integer.toString(id)});
                rowCount++;
            }

            Log.i(TAG, "end populateUUID rows updated:" + rowCount);
        } catch(Exception e) {
            throw new SyncException(SyncStatus.ERROR_GENERATE_UUID, "Error generating UUID message:" + e.getMessage());
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }


    private void readDatabase(final InputStream inputStream) {
        JSonDataBaseReader reader;
        DatabaseReaded dbReaded;
        TableReaded dbTable;
        Record dbRecord;
        Map<String, ColumnMetadata> columns = null;

        try {
            System.out.println("-----");

            reader = new JSonDataBaseReader(inputStream);

            int elementType;
            while ((elementType = reader.nextElement()) != JSonDataBaseReader.END) {
                System.out.println(elementType);
                switch (elementType) {
                    case JSonDataBaseReader.START_DB:
                        dbReaded = reader.readDatabase();
                        System.out.println(dbReaded.toString());
                        break;
                    case JSonDataBaseReader.START_TABLE:
                        dbTable = reader.readTable();
                        System.out.println(dbTable.toString());
                        columns = SqlLiteUtility.readTableMetadataAsMap(mDB, dbTable.getName());
                        break;
                    case JSonDataBaseReader.RECORD:
                        dbRecord = reader.readRecord(columns);
                        System.out.println(dbRecord.toString());
                        break;
                }
            }

            System.out.println("-----");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }


    }
    public static class Builder {

        private CloudProvider mCloudProvider;
        private SQLiteDatabase mDB;
        private String mDataBaseName;
        private List<Table> mTables = new ArrayList<>();
        private Context mCtx;

        public Builder(final Context ctx) {
            this.mCtx = ctx;
        }

        public Builder setCloudProvider(final CloudProvider cloudProvider) {
            mCloudProvider = cloudProvider;
            return this;
        }

        public Builder setSQLiteDatabase(final SQLiteDatabase db) {
            this.mDB = db;
            return this;
        }

        public Builder addTable(final Table table) {
            this.mTables.add(table);
            return this;
        }


        public Builder setDataBaseName(String dataBaseName) {
            this.mDataBaseName = dataBaseName;
            return this;
        }

        public DBSync build(){
            return new DBSync(mCtx, mCloudProvider, mDB, mDataBaseName, mTables);
        }
    }
}
