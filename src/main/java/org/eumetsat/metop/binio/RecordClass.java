/* 
 * Copyright (C) 2002-2007 by Eumetsat
 */
package org.eumetsat.metop.binio;


enum RecordClass {
    RESERVED,
    MPHR,
    SPHR,
    IPR,
    GEADR,
    GIADR,
    VEADR,
    VIADR,
    MDR;

    public static RecordClass createRecordClass(int index) {
        if (isValid(index)) {
            return values()[index];
        }
        return null;
    }

    private static boolean isValid(int index) {
        return (index >= 0 && index < values().length);
    }
}
