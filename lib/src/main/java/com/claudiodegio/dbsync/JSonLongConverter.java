package com.claudiodegio.dbsync;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;


public class JSonLongConverter implements JSonConverter {
    @Override
    public ColumnValue jsonToColumnValue(final JsonParser parser, final ColumnMetadata metadata) throws IOException {

        JsonToken token;
        ColumnValue value;

        // Go to the next token
        token = parser.nextToken();

        if (token != JsonToken.VALUE_NUMBER_INT && token != JsonToken.VALUE_NULL) {
            throw new IOException("Unable to parse field " + metadata.getName() + " expected int or null at line " + parser.getCurrentLocation().getLineNr());
        }

        // Can be a integer or null
        if (token == JsonToken.VALUE_NUMBER_INT) {
            value = new ColumnValue(parser.getValueAsLong(), metadata);
        } else {
            // null
            if (metadata.isNotNull()) {
                throw new IOException("Unable to parse field " + metadata.getName() + " expected int bu found null at line " + parser.getCurrentLocation().getLineNr());
            }

            value = new ColumnValue(metadata);
        }

        return value;
    }
}
