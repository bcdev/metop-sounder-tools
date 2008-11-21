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
 * @version $Revision: 79 $ $Date: 2008-01-10 11:23:38 +0100 (Do, 10 Jan 2008) $
 */
class GiadrQuality {

    private static final int PN = 4; //number of sounder pixels

    int[] defPsfSondNbLin;
    int[] defPsfSondNbCol;

    public void readRecord(ImageInputStream inputStream) throws IOException {
        GenericRecordHeader grh = GenericRecordHeader.readGenericRecordHeader(inputStream);

        if (grh.recordClass != RecordClass.GIADR
                || grh.instrumentGroup != InstrumentGroup.IASI
                || grh.recordSubclass != 0) {
            throw new IllegalArgumentException("Bad GRH.");
        }

        defPsfSondNbLin = new int[PN];
        for (int i = 0; i < defPsfSondNbLin.length; i++) {
            defPsfSondNbLin[i] = inputStream.readInt();
        }
        defPsfSondNbCol = new int[PN];
        for (int i = 0; i < defPsfSondNbCol.length; i++) {
            defPsfSondNbCol[i] = inputStream.readInt();
        }
        byte readByte = inputStream.readByte();
        int readInt = inputStream.readInt();

    }
}
