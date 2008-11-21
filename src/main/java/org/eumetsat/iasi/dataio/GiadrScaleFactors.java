/*
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.iasi.dataio;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @version $Revision: 79 $ $Date: 2008-01-10 11:23:38 +0100 (Do, 10 Jan 2008) $
 */
class GiadrScaleFactors {
    private static final int PN = 4; //number of sounder pixel
    private static final int SS = 8700;

    private short defScaleSondNbScale;
    private short[] defScaleSondNsfirst = new short[10];
    private short[] defScaleSondNslast = new short[10];
    private short[] defScaleSondScaleFactor = new short[10];
    private short defScaleIISScaleFactor;

    public void readRecord(ImageInputStream inputStream) throws IOException {
        GenericRecordHeader grh = GenericRecordHeader.readGenericRecordHeader(inputStream);
        if (grh.recordClass != RecordClass.GIADR
                || grh.instrumentGroup != InstrumentGroup.IASI
                || grh.recordSubclass != 1) {
            throw new IllegalArgumentException("Bad GRH.");
        }

        defScaleSondNbScale = inputStream.readShort();
        for (int i = 0; i < 10; i++) {
            defScaleSondNsfirst[i] = inputStream.readShort();
        }
        for (int i = 0; i < 10; i++) {
            defScaleSondNslast[i] = inputStream.readShort();
        }
        for (int i = 0; i < 10; i++) {
            defScaleSondScaleFactor[i] = inputStream.readShort();
        }
        defScaleIISScaleFactor = inputStream.readShort();
    }

    public double[] getScaleFactors(int first) {
        double[] scaleFactors = new double[SS];
        Arrays.fill(scaleFactors, 1);
        for (int i = 0; i < defScaleSondNbScale; i++) {
            int start = defScaleSondNsfirst[i] - first;
            int end = defScaleSondNslast[i] - first + 1;
            short sfe = defScaleSondScaleFactor[i];
            double sf = 1.0 / (Math.pow(10.0, sfe));
            Arrays.fill(scaleFactors, start, end, sf);
        }
        return scaleFactors;
    }
}
