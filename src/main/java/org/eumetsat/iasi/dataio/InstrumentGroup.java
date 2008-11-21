/* 
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.iasi.dataio;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 */
enum InstrumentGroup {
    GENERIC,
    AMSU_A,
    ASCAT,
    ATOVS,
    AVHRR_3,
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

    public static InstrumentGroup readInstrumentGroup(ImageInputStream iis) throws IOException {
        final byte b = iis.readByte();

        if (isValid(b)) {
            return values()[b];
        }

        throw new IOException("unknown instrument group '" + b + "'");
    }

    private static boolean isValid(byte b) {
        return (b >= 0 && b < values().length);
    }
}
