/*
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.iasi.dataio;

import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @version $Revision: 79 $ $Date: 2008-01-10 11:23:38 +0100 (Do, 10 Jan 2008) $
 */
class GenericRecordHeader {

    public RecordClass recordClass;
    public InstrumentGroup instrumentGroup;
    public byte recordSubclass;
    public byte recordSubclassVersion;
    public long recordSize;
    public ProductData.UTC recordStartTime;
    public ProductData.UTC recordEndTime;

    public static GenericRecordHeader readGenericRecordHeader(ImageInputStream iis) throws IOException {
        final GenericRecordHeader grh = new GenericRecordHeader();

        grh.recordClass = RecordClass.readRecordClass(iis);
        grh.instrumentGroup = InstrumentGroup.readInstrumentGroup(iis);
        grh.recordSubclass = iis.readByte();
        grh.recordSubclassVersion = iis.readByte();
        grh.recordSize = iis.readUnsignedInt();
        grh.recordStartTime = EpsMetopUtil.readShortCdsTime(iis);
        grh.recordEndTime = EpsMetopUtil.readShortCdsTime(iis);

        return grh;
    }

    void printGenericRecordHeader() {
        System.out.println("GRH");
        System.out.println("recordClass = " + recordClass);
        System.out.println("instrumentGroup = " + instrumentGroup);
        System.out.println("recordSubclass = " + recordSubclass);
        System.out.println("recordSubclassVersion = " + recordSubclassVersion);
        System.out.println("recordSize = " + recordSize);
    }
}
