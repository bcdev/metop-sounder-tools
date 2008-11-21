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
class InternalPointerRecord {

    public GenericRecordHeader grh;
    public RecordClass targetRecordClass;
    public InstrumentGroup targetInstrumentGroup;
    public byte targetRecordSubclass;
    public long targetRecordOffset;

    public static InternalPointerRecord readInternalPointerRecord(ImageInputStream iis) throws IOException {
        final InternalPointerRecord ipr = new InternalPointerRecord();

        ipr.grh = GenericRecordHeader.readGenericRecordHeader(iis);

        if (ipr.grh.recordClass != RecordClass.IPR
                || ipr.grh.instrumentGroup != InstrumentGroup.GENERIC) {
            throw new IOException("Illegal Generic Record Header");
        }

        ipr.targetRecordClass = RecordClass.readRecordClass(iis);
        ipr.targetInstrumentGroup = InstrumentGroup.readInstrumentGroup(iis);
        ipr.targetRecordSubclass = iis.readByte();
        ipr.targetRecordOffset = iis.readUnsignedInt();

        return ipr;
    }

    void printInternalPointerRecord() {
        System.out.println("IPR");
        System.out.println("targetRecordClass " + targetRecordClass);
        System.out.println("targetInstrumentGroup " + targetInstrumentGroup);
        System.out.println("targetRecordSubclass " + targetRecordSubclass);
        System.out.println("targetRecordOffset " + targetRecordOffset);
    }
}
