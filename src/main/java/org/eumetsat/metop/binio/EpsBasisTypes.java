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

import static com.bc.ceres.binio.TypeBuilder.*;

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.core.Assert;

/**
 * Defines the formats for all METOP product types in EPS format.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 */
public class EpsBasisTypes {
    
    private static final CompoundType SHORT_CDS_TIME = 
        COMPOUND("Short_CDS_Time",
             MEMBER("Day", USHORT),
             MEMBER("Millisec_In_Day", UINT)
        );
    
    private static final CompoundType GRH =
        COMPOUND("Generic_Record_Header",
             MEMBER("Record_Class", UBYTE),
             MEMBER("Instrument_Group", UBYTE),
             MEMBER("Record_Subclass", UBYTE),
             MEMBER("Record_Subclass_Version", UBYTE),
             MEMBER("Record_Size", UINT),
             MEMBER("Record_Start_Time", SHORT_CDS_TIME),
             MEMBER("Record_End_Time", SHORT_CDS_TIME)
        );
    
    private static final CompoundType POINTER =
        COMPOUND("Generic_Record_Pointer",
             MEMBER("Target_Record_Class", UBYTE),
             MEMBER("Target_Instrument_Group", UBYTE),
             MEMBER("Target_Record_Subclass", UBYTE),
             MEMBER("Taregt_Record_Offset", UINT)
        );

    private static final EpsBasisTypes INSTANCE = new EpsBasisTypes();
    private DataFormat format;
    
    public static EpsBasisTypes getInstance() {
        return INSTANCE;
    }
    
    public DataFormat getFormat() {
        return format;
    }
    
    public static String buildTypeName(RecordClass recordClass, InstrumentGroup instrumentGroup, int subClass) {
        Assert.notNull(recordClass, "recordClass");
        Assert.notNull(instrumentGroup, "instrumentGroup");
        StringBuilder sb = new StringBuilder(recordClass.toString());
        sb.append(":");
        sb.append(instrumentGroup.toString());
        sb.append(":");
        sb.append(subClass);
        return sb.toString();
    }
    
    private EpsBasisTypes() {
        format = new DataFormat();
        format.setName("EPS Basis Types");
        format.addTypeDef("byte", SimpleType.BYTE);
        format.addTypeDef("ubyte", SimpleType.UBYTE);
        format.addTypeDef("enumerated", SimpleType.UBYTE);
        format.addTypeDef("boolean", SimpleType.BYTE);
        format.addTypeDef("integer1", SimpleType.BYTE);
        format.addTypeDef("uinteger1", SimpleType.UBYTE);
        format.addTypeDef("integer2", SimpleType.SHORT);
        format.addTypeDef("uinteger2", SimpleType.USHORT);
        format.addTypeDef("integer4", SimpleType.INT);
        format.addTypeDef("uinteger4", SimpleType.UINT);
        format.addTypeDef("integer8", SimpleType.LONG);
        format.addTypeDef("uinteger8", SimpleType.ULONG);

        format.addTypeDef("vbyte", createVType("vbyte", SimpleType.BYTE));
        format.addTypeDef("vubyte", createVType("vbyte", SimpleType.UBYTE));
        format.addTypeDef("vinteger1", createVType("vbyte", SimpleType.BYTE));
        format.addTypeDef("vuinteger1", createVType("vbyte", SimpleType.UBYTE));
        format.addTypeDef("vinteger2", createVType("vbyte", SimpleType.SHORT));
        format.addTypeDef("vuinteger2", createVType("vbyte", SimpleType.USHORT));
        format.addTypeDef("vinteger4", createVType("vbyte", SimpleType.INT));
        format.addTypeDef("vuinteger4", createVType("vbyte", SimpleType.UINT));
        format.addTypeDef("vinteger8", createVType("vbyte", SimpleType.LONG));
        format.addTypeDef("vuinteger8", createVType("vbyte", SimpleType.LONG));
        
        format.addTypeDef("short_cds_time", SHORT_CDS_TIME);
        format.addTypeDef("time", SHORT_CDS_TIME);
        format.addTypeDef("grh", GRH);
        format.addTypeDef("pointer", POINTER);
        
        CompoundMember auxDataPointer = MEMBER("Aux_Data_Pointer", SEQUENCE(BYTE, 100));
        EpsMetatData auxDataPointerMetatData = new EpsMetatData();
        auxDataPointerMetatData.setType("string");
        auxDataPointer.setMetadata(auxDataPointerMetatData);
        format.addTypeDef("geadr", COMPOUND("geadr", auxDataPointer));
        format.addTypeDef("veadr", COMPOUND("veadr", auxDataPointer));
        
    }
    
    private static Type createVType(String name, SimpleType type) {
        return COMPOUND(name, MEMBER("scale", SimpleType.BYTE), MEMBER("value", type));
    }
}
