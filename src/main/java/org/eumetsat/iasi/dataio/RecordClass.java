/* 
 * Copyright (C) 2002-2007 by Eumetsat
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

    public static RecordClass readRecordClass(ImageInputStream iis) throws IOException {
        final byte b = iis.readByte();

        if (isValid(b)) {
            return values()[b];
        }

        throw new IOException("unknown record class '" + b + "'");
    }

    private static boolean isValid(byte b) {
        return (b >= 0 && b < values().length);
    }
}
