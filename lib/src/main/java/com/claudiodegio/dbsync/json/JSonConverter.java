package com.claudiodegio.dbsync.json;

import com.claudiodegio.dbsync.core.ValueMetadata;
import com.claudiodegio.dbsync.core.Value;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

/**
 * Json Converter class for convert from db -> json and json -> to db
 */
public interface JSonConverter {

    /**
     * Convert json value to column value
     * @param parser the parser
     * @param metadata the column metadata
     * @return the value of columns
     * @throws IOException
     */
    Value jsonToColumnValue(JsonParser parser, ValueMetadata metadata) throws IOException;

    /**
     * Convert column value to json (write directly into generator=
     * @param gen the generator to use
     * @param value the value to write
     * @throws IOException
     */
    void columnValueToJson(JsonGenerator gen, Value value) throws IOException;
}
