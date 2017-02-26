package com.claudiodegio.dbsync.json;

import com.claudiodegio.dbsync.core.ColumnMetadata;
import com.claudiodegio.dbsync.core.ColumnValue;
import com.fasterxml.jackson.core.JsonGenerator;
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

    @Override
    public void columnValueToJson(JsonGenerator gen, ColumnValue value) throws IOException {

        String fieldName;

        fieldName = value.getMetadata().getName();

        if (value.isNull()) {
            gen.writeNullField(fieldName);
        } else {
            gen.writeNumberField(fieldName, value.getValueLong());
        }
    }
}
