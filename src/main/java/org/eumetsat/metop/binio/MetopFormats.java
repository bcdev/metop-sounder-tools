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

import static com.bc.ceres.binio.util.TypeBuilder.BYTE;
import static com.bc.ceres.binio.util.TypeBuilder.COMP;
import static com.bc.ceres.binio.util.TypeBuilder.MEMBER;
import static com.bc.ceres.binio.util.TypeBuilder.SEQ;
import static com.bc.ceres.binio.util.TypeBuilder.UBYTE;
import static com.bc.ceres.binio.util.TypeBuilder.UINT;
import static com.bc.ceres.binio.util.TypeBuilder.USHORT;

import java.io.IOException;
import java.util.HashMap;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.Format;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SequenceTypeMapper;
import com.bc.ceres.binio.util.SequenceElementCountResolver;

/**
 * Defines the formats of all supported METOP product types.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 */
public class MetopFormats {
    public static final CompoundType SHORT_CDS_TIME = 
        COMP("Short_CDS_Time",
             MEMBER("Day", USHORT),
             MEMBER("Millisec_In_Day", UINT)
        );
    
    public static final CompoundType REC_HEAD =
        COMP("Generic_Record_Header",
             MEMBER("Record_Class", UBYTE),
             MEMBER("Instrument_Group", UBYTE),
             MEMBER("Record_Subclass", UBYTE),
             MEMBER("Record_Subclass_Version", UBYTE),
             MEMBER("Record_Size", UINT),
             MEMBER("Record_Start_Time", SHORT_CDS_TIME),
             MEMBER("Record_End_Time", SHORT_CDS_TIME)
        );
    
    public static final CompoundType POINTER =
        COMP("Generic_Record_Pointer",
             MEMBER("Target_Record_Class", UBYTE),
             MEMBER("Target_Instrument_Group", UBYTE),
             MEMBER("Target_Record_Subclass", UBYTE),
             MEMBER("Taregt_Record_Offset", UINT)
        );
    
    public static final CompoundType PRODUCT_DETAILS =
        COMP("Product_Details",
             MEMBER("Product_Name", SEQ(BYTE, 100)),
             MEMBER("Parent_Product_Name1", SEQ(BYTE, 100)),
             MEMBER("Parent_Product_Name2", SEQ(BYTE, 100)),
             MEMBER("Parent_Product_Name4", SEQ(BYTE, 100)),
             MEMBER("Instrument_ID", SEQ(BYTE, 37)),
             MEMBER("Instrument_Model", SEQ(BYTE, 36)),
             MEMBER("Product_Type", SEQ(BYTE, 36)),
             MEMBER("Processing_Level", SEQ(BYTE, 35)),
             MEMBER("Spacecraft_ID", SEQ(BYTE, 36))
        );
    
    
    public static final CompoundType MPHR =
        COMP("Main_Product_Header_Record",
             MEMBER("GRH", REC_HEAD),
             MEMBER("Product_Details", PRODUCT_DETAILS)
        );
    
    public static final CompoundType METOP_RECORD =
        COMP("MR",
             MEMBER("GRH", REC_HEAD),
             MEMBER("recordData", SEQ(BYTE))
        );
    
    public static final CompoundType METOP_FILE = 
        COMP("METOP_FILE", 
//             MEMBER("all", SEQ(METOP_RECORD))
//             MEMBER("all", METOP_RECORD),
             MEMBER("record1", MPHR)
//             MEMBER("record2", METOP_RECORD),
//             MEMBER("record3", METOP_RECORD),
//             MEMBER("record4", METOP_RECORD),
//             MEMBER("record5", METOP_RECORD)
        );

    
    public static MetopFormats getInstance() {
        return INSTANCE;
    }
    
    public Format getFormat() {
        return formatMap.get("FOO");
    }
    private static final MetopFormats INSTANCE = new MetopFormats();
    private HashMap<String, Format> formatMap;
    
    private MetopFormats() {
        formatMap = new HashMap<String, Format>(17);
        
        Format metopFormat = new Format(MetopFormats.METOP_FILE);
        final SequenceTypeMapper elementCountResolver = new SequenceElementCountResolver() {
            
            @Override
            public int getElementCount(CollectionData collectionData, SequenceType sequenceType) throws IOException {
                final long size = collectionData.getCompound(0).getUInt(4);
                return (int) (size - 20);
            }
        };
        metopFormat.addSequenceTypeMapper(MetopFormats.METOP_RECORD.getMember(1), elementCountResolver);
        formatMap.put("FOO", metopFormat);
    }
}
