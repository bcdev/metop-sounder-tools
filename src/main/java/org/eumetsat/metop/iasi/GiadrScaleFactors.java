/*
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.metop.iasi;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.SequenceData;

import org.eumetsat.metop.eps.GenericRecordHeader;
import org.eumetsat.metop.eps.InstrumentGroup;
import org.eumetsat.metop.eps.RecordClass;

import java.io.IOException;
import java.util.Arrays;


class GiadrScaleFactors {
    private static final int PN = 4; //number of sounder pixel
    private static final int SS = 8700;

    private short defScaleSondNbScale;
    private short[] defScaleSondNsfirst;
    private short[] defScaleSondNslast;
    private short[] defScaleSondScaleFactor;
    private short defScaleIISScaleFactor;

    public GiadrScaleFactors(CompoundData data) throws IOException {
        GenericRecordHeader grh = new GenericRecordHeader(data.getCompound(0));
        if (grh.recordClass != RecordClass.GIADR
                || grh.instrumentGroup != InstrumentGroup.IASI
                || grh.recordSubclass != 1) {
            throw new IllegalArgumentException("Bad GRH.");
        }
        CompoundData body = data.getCompound(1);
        defScaleSondNbScale = body.getShort(0);
        defScaleSondNsfirst = readShortArray(body.getSequence(1));
        defScaleSondNslast = readShortArray(body.getSequence(2));
        defScaleSondScaleFactor = readShortArray(body.getSequence(3));
        defScaleIISScaleFactor = body.getShort(4);
    }
    
    private short[] readShortArray(SequenceData sequenceData) throws IOException {
        int elementCount = sequenceData.getElementCount();
        short[] data = new short[elementCount];
        for (int i = 0; i < data.length; i++) {
            data[i] = sequenceData.getShort(i);
        }
        return data;
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
