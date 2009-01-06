/* 
 * Copyright (C) 2002-2007 by Eumetsat
 */
package org.eumetsat.metop.binio;

import java.util.HashMap;
import java.util.Map;

enum RecordClass {
    RESERVED(0, false),
    MPHR(1, true),
    SPHR(2, true),
    IPR(3, false),
    GEADR(4, false),
    GIADR(5, false),
    VEADR(6, false),
    VIADR(7, false),
    MDR(8, false);
    
    private final int value;
    private final boolean isAscii;
    private static final Map<String, RecordClass> stringToEnum = new HashMap<String, RecordClass>(16);
    private static final Map<Integer, RecordClass> intToEnum = new HashMap<Integer, RecordClass>(16);
    
    static {
        for (RecordClass rc : values()) {
            stringToEnum.put(rc.toString(), rc);
            intToEnum.put(rc.value, rc);
        }
    }

    private RecordClass(int value, boolean isAscii) {
        this.value = value;
        this.isAscii = isAscii;
    }
    
    public static RecordClass fromString(String name) {
        return stringToEnum.get(name.toLowerCase());
    }
    
    public static RecordClass fromInt(int index) {
        return intToEnum.get(index);
    }
    
    public boolean isAscii() {
        return isAscii;
    }
    
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
