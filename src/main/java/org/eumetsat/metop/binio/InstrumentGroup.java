/* 
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.metop.binio;

import java.util.HashMap;
import java.util.Map;

enum InstrumentGroup {
    GENERIC(0),
    AMSUA(1),
    ASCAT(2),
    ATOVS(3),
    AVHRR(4),
    GOME(5),
    GRAS(6),
    HIRS4(7),
    IASI(8),
    MHS(9),
    SEM(10),
    ADCS(11),
    SBUV(12),
    DUMMY(13),
    ARCHIVE(14),
    IASIL2(15);

    private final int value;
    private static final Map<String, InstrumentGroup> stringToEnum = new HashMap<String, InstrumentGroup>(32);
    private static final Map<Integer, InstrumentGroup> intToEnum = new HashMap<Integer, InstrumentGroup>(32);
    
    static {
        for (InstrumentGroup ig : values()) {
            stringToEnum.put(ig.toString(), ig);
            intToEnum.put(ig.value, ig);
        }
    }
    
    private InstrumentGroup(int value) {
        this.value = value;
    }

    public static InstrumentGroup fromString(String name) {
        return stringToEnum.get(name.toLowerCase());
    }
    
    public static InstrumentGroup fromInt(int index) {
        return intToEnum.get(index);
    }
    
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
