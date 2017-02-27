package com.claudiodegio.dbsync.json;

import com.claudiodegio.dbsync.core.ValueMetadata;

/**
 * Factory for create right converter
 */
public class JSonConverterFactory {

    static JSonConverter buildConverter(final ValueMetadata metadata) {
        switch (metadata.getType()) {
            case ValueMetadata.TYPE_LONG:
                return new JSonLongConverter();
            case ValueMetadata.TYPE_STRING:
                return new JSonStringConverter();
            default:
                throw new RuntimeException("Format non supported");
        }
    }

}
