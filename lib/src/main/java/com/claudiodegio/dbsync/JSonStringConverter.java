package com.claudiodegio.dbsync;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;


public class JSonStringConverter implements JSonConverter {
    @Override
    public ColumnValue jsonToColumnValue(JsonParser parser, ColumnMetadata metadata) throws IOException {
        JsonToken token;
        ColumnValue value;

        // Go to the next token
        token = parser.nextToken();

        if (token != JsonToken.VALUE_STRING && token != JsonToken.VALUE_NULL) {
            throw new IOException("Unable to parse field " + metadata.getName() + " expected string or null at line " + parser.getCurrentLocation().getLineNr());
        }

        // Can be a integer or null
        if (token == JsonToken.VALUE_STRING) {
            value = new ColumnValue(parser.getValueAsString(), metadata);
        } else {
            // null
            if (metadata.isNotNull()) {
                throw new IOException("Unable to parse field " + metadata.getName() + " expected string but found null at line " + parser.getCurrentLocation().getLineNr());
            }

            value = new ColumnValue(metadata);
        }

        return value;
    }
}
