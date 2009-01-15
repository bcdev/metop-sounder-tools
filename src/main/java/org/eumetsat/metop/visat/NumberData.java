/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.eumetsat.metop.visat;

import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;

import java.io.IOException;
import java.text.MessageFormat;

abstract class NumberData {
    private final SequenceData sequenceData;

    static NumberData create(SequenceData sequenceData) {
        final Type elementType = sequenceData.getSequenceType().getElementType();

        if (elementType == SimpleType.BYTE) {
            return new NumberData(sequenceData) {
                @Override
                public final Number getNumber(int index) throws IOException {
                    return getSequenceData().getByte(index);
                }
            };
        }
        if (elementType == SimpleType.UBYTE) {
            return new NumberData(sequenceData) {
                @Override
                public final Number getNumber(int index) throws IOException {
                    return getSequenceData().getUByte(index);
                }
            };
        }
        if (elementType == SimpleType.SHORT) {
            return new NumberData(sequenceData) {
                @Override
                public final Number getNumber(int index) throws IOException {
                    return getSequenceData().getShort(index);
                }
            };
        }
        if (elementType == SimpleType.USHORT) {
            return new NumberData(sequenceData) {
                @Override
                public final Number getNumber(int index) throws IOException {
                    return getSequenceData().getUShort(index);
                }
            };
        }
        if (elementType == SimpleType.INT) {
            return new NumberData(sequenceData) {
                @Override
                public final Number getNumber(int index) throws IOException {
                    return getSequenceData().getInt(index);
                }
            };
        }
        if (elementType == SimpleType.UINT) {
            return new NumberData(sequenceData) {
                @Override
                public final Number getNumber(int index) throws IOException {
                    return getSequenceData().getUInt(index);
                }
            };
        }
        if (elementType == SimpleType.LONG) {
            return new NumberData(sequenceData) {
                @Override
                public final Number getNumber(int index) throws IOException {
                    return getSequenceData().getLong(index);
                }
            };
        }
        if (elementType == SimpleType.FLOAT) {
            return new NumberData(sequenceData) {
                @Override
                public final Number getNumber(int index) throws IOException {
                    return getSequenceData().getFloat(index);
                }
            };
        }
        if (elementType == SimpleType.DOUBLE) {
            return new NumberData(sequenceData) {
                @Override
                public final Number getNumber(int index) throws IOException {
                    return getSequenceData().getDouble(index);
                }
            };
        }

        throw new IllegalArgumentException(MessageFormat.format(
                "Unsupported sequence type: {0}", sequenceData.getSequenceType().getName()));
    }

    private NumberData(SequenceData sequenceData) {
        this.sequenceData = sequenceData;
    }

    final SequenceData getSequenceData() {
        return sequenceData;
    }

    final int getElementCount() {
        return sequenceData.getElementCount();
    }

    abstract Number getNumber(int index) throws IOException;
}
