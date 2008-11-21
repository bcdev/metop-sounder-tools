/*
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.iasi.dataio;

import org.esa.beam.framework.datamodel.MetadataElement;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.*;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @version $Revision: 79 $ $Date: 2008-01-10 11:23:38 +0100 (Do, 10 Jan 2008) $
 */
abstract class AsciiRecord {

    private final Map<String, String> map;
    private final int fieldCount;

    protected AsciiRecord(int fieldCount) {
        this.map = new HashMap<String, String>();
        this.fieldCount = fieldCount;
    }

    public void readRecord(ImageInputStream iis) throws IOException {
        for (int i = 0; i < fieldCount; i++) {
            final String fieldString = iis.readLine();
            final KeyValuePair field = new KeyValuePair(fieldString);

            map.put(field.key, field.value);
        }
    }

    public String getValue(String key) {
        return map.get(key);
    }

    public int getIntValue(String key) {
        return Integer.parseInt(getValue(key));
    }

    public long getLongValue(String key) {
        return Long.parseLong(getValue(key));
    }

    public abstract MetadataElement getMetaData();

    public void printValues() {
        final List<String> keyList = new ArrayList<String>(map.keySet());
        Collections.sort(keyList);

        for (final String key : keyList) {
            System.out.println(key + "=" + map.get(key));
        }
    }

    private static class KeyValuePair {
        final String key;
        final String value;

        public KeyValuePair(String field) {
            key = field.substring(0, 30).trim();
            value = field.substring(32).trim();
        }
    }
}
