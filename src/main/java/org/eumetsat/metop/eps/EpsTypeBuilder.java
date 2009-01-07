/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.eumetsat.metop.binio;

import static com.bc.ceres.binio.TypeBuilder.BYTE;
import static com.bc.ceres.binio.TypeBuilder.COMPOUND;
import static com.bc.ceres.binio.TypeBuilder.MEMBER;
import static com.bc.ceres.binio.TypeBuilder.SEQUENCE;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.IOHandler;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.VarSequenceType;
import com.bc.ceres.binio.internal.AbstractType;
import com.bc.ceres.binio.internal.VarElementCountSequenceType;
import com.bc.ceres.binio.util.RandomAccessFileIOHandler;

import java.io.IOException;


public class EpsTypeBuilder {

    private final String name;
    private final DataFormat format;

    public EpsTypeBuilder(String name, DataFormat format) {
        this.name = name;
        this.format = format;
    }
    
    public CompoundType buildProduct() {
        return COMPOUND(name, 
                        MEMBER("Header", createHeaderType()), 
                        MEMBER("Pointer", createPointerType()),
                        MEMBER("Body", createBodyType()));
    }
    
    public CompoundType buildMPHR() {
        return createRecord("MPHR", format.getTypeDef("mphr"));
    }

    private Type createHeaderType() {
        CompoundType headerType;
        Type mphr = createRecord("MPHR", format.getTypeDef("mphr"));
        if (!format.isTypeDef("sphr")) {
            headerType = COMPOUND("HEADER_TYPE", MEMBER("mphr", mphr));
        } else {
            Type sphr = createRecord("SPHR", format.getTypeDef("sphr"));
            headerType = COMPOUND("HEADER_TYPE", MEMBER("mphr", mphr), MEMBER("sphr", sphr));
        }
        return headerType;
    }

    private Type createPointerType() {
        CompoundType recordType = createRecord("ipr", format.getTypeDef("pointer"));
        return new IPRElementCountSequenceType(recordType);
    }
    
    private Type createBodyType() {
        return new BodySequenceType();
    }
    
    private CompoundType createRecord(String recordName, Type bodytype) {
        return COMPOUND(recordName, 
                        MEMBER("header", format.getTypeDef("grh")), 
                        MEMBER("body", bodytype));
    }
    
    private class IPRElementCountSequenceType extends VarElementCountSequenceType {

        protected IPRElementCountSequenceType(Type elementType) {
            super(elementType);
        }
        
        @Override
        protected int resolveElementCount(CollectionData parentData) throws IOException {
            DataContext context = parentData.getContext();
            CompoundData headerData = parentData.getCompound(0);
            long headerSize = headerData.getSize();
            CompoundType pointerRecordType = (CompoundType) getElementType();

            long offset = headerSize;
            boolean done = false;
            int count = 0;
            while (!done) {
                CompoundData iprData = context.getData(pointerRecordType, offset);
                byte recordClassValue = iprData.getCompound("header").getByte("Record_Class");
                RecordClass recordClass = RecordClass.createRecordClass(recordClassValue);
                if (recordClass == RecordClass.IPR) {
                    count++;
                    offset += iprData.getSize();
                } else {
                    done = true;
                }
            }
            return count;
        }
    }
    
    private class BodySequenceType extends AbstractType implements VarSequenceType {
        
        @Override
        public SequenceType resolve(CollectionData parent) throws IOException {
            Type bodyType = createBodyType(parent.getSequence(1));
            return SEQUENCE(bodyType, 1);
        }

        @Override
        public int getElementCount() {
            return 1;
        }

        @Override
        public Type getElementType() {
            return BYTE;
        }

        @Override
        public String getName() {
            return "DATA_BODY";
        }

        @Override
        public int getSize() {
            return -1;
        }
        
        @Override
        public boolean isSequenceType() {
            return true;
        }

        private Type createBodyType(CollectionData iprData) throws IOException {
            int iprCount = iprData.getElementCount();
            CompoundMember[] members = new CompoundMember[iprCount];
            InternalPointerRecord[] allIPRs = new InternalPointerRecord[iprCount];
            for (int pointerIndex = 0; pointerIndex < members.length; pointerIndex++) {
                CompoundData pointerRecord = iprData.getCompound(pointerIndex).getCompound(1);
                allIPRs[pointerIndex] = new InternalPointerRecord(pointerRecord);
            }
            for (int pointerIndex = 0; pointerIndex < members.length; pointerIndex++) {
                InternalPointerRecord ipr = allIPRs[pointerIndex];
                long start = ipr.getRecordOffset();
                long end;
                int count = 0;
                Type type = null;
                String typeName = EpsBasisTypes.buildTypeName(ipr.getRecordClass(), ipr.getInstrumentGroup(), ipr.getRecordSubclass());
                if (format.isTypeDef(typeName)) {
                    type = format.getTypeDef(typeName);
                } else {
                    typeName = ipr.getRecordClass().toString().toLowerCase();
                    if (format.isTypeDef(typeName)) {
                        type = format.getTypeDef(typeName);
                    }
                }
                if (isLastPointer(members.length, pointerIndex)) {
                    //count = 1;
                    end = start + type.getSize() + 20;
                    //IOHandler handler = iprData.getContext().getHandler();
                    //end = handler.getMaxPosition();
                } else {
                    end = allIPRs[pointerIndex+1].getRecordOffset();
                }
                long size = (end - start);
                if (type == null) {
                    type = SEQUENCE(BYTE, (int)size-20);
                    typeName = "dummy";
                }
                count = (int) (size /(type.getSize() + 20));
                members[pointerIndex] = MEMBER(typeName, SEQUENCE(createRecord(typeName, type), count));
            }
            return COMPOUND("BODY_TYPE", members);
        }

        private boolean isLastPointer(int numPointers, int i) {
            return i == numPointers - 1;
        }
    }
}
