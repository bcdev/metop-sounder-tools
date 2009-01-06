/*
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.metop.binio;

import com.bc.ceres.binio.CompoundData;

import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;


public class GenericRecordHeader {

    public final RecordClass recordClass;
    public final InstrumentGroup instrumentGroup;
    public final byte recordSubclass;
    public final byte recordSubclassVersion;
    public final long recordSize;
    public final ProductData.UTC recordStartTime;
    public final ProductData.UTC recordEndTime;

    public GenericRecordHeader(CompoundData data) throws IOException {
        recordClass = RecordClass.fromInt(data.getInt(0));
        instrumentGroup = InstrumentGroup.fromInt(data.getInt(1));
        recordSubclass = data.getByte(2);
        recordSubclassVersion = data.getByte(3);
        recordSize = data.getByte(4);
        recordStartTime = EpsFile.readShortCdsTime(data.getCompound(5));
        recordEndTime = EpsFile.readShortCdsTime(data.getCompound(6));
    }
    

}
