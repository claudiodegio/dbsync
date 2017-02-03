package com.claudiodegio.dbsync;

public class JSonConverterFactory {

    static JSonConverter buildConverter(final ColumnMetadata metadata) {
        switch (metadata.getType()) {
            case ColumnMetadata.TYPE_LONG:
                return new JSonLongConverter();
            case ColumnMetadata.TYPE_STRING:
                return new JSonStringConverter();
            default:
                throw new RuntimeException("Format non supported");
        }
    }

}
