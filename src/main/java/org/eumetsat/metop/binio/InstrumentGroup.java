/* 
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.metop.binio;

enum InstrumentGroup {
    GENERIC,
    AMSU_A,
    ASCAT,
    ATOVS,
    AVHRR,
    GOME,
    GRAS,
    HIRS_4,
    IASI,
    MHS,
    SEM,
    ADCS,
    SBUV,
    DUMMY,
    ARCHIVE,
    IASI_L2;

    public static InstrumentGroup createInstrumentGroup(int index) {
        if (isValid(index)) {
            return values()[index];
        }
        return null;
    }

    private static boolean isValid(int index) {
        return (index >= 0 && index < values().length);
    }
}
