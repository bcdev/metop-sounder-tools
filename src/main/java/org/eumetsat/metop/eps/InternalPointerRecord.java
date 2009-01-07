/* 
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.metop.eps;

import java.io.IOException;

import com.bc.ceres.binio.CompoundData;

class InternalPointerRecord {

    private final RecordClass recordClass;
    private final InstrumentGroup instrumentGroup;
    private final byte recordSubclass;
    private final long rRecordOffset;

    public InternalPointerRecord(CompoundData data) throws IOException {
        this.recordClass = RecordClass.fromInt(data.getInt(0));
        this.instrumentGroup = InstrumentGroup.fromInt(data.getInt(1));
        this.recordSubclass = data.getByte(2);
        this.rRecordOffset = data.getLong(3);
    }

    public RecordClass getRecordClass() {
        return recordClass;
    }
    
    public InstrumentGroup getInstrumentGroup() {
        return instrumentGroup;
    }
    
    public int getRecordSubclass() {
        return recordSubclass;
    }
    
    public long getRecordOffset() {
        return rRecordOffset;
    }
    
    @Override
    public String toString() {
        return "IPR ["+ recordClass +
        "," + instrumentGroup +
        "," + recordSubclass + 
        "," + rRecordOffset+"]";
    }
}
