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

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;

/**
 * Defines the formats of all supported METOP product types.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 */
public class EpsBasisFormats {
    
    public static final CompoundType SHORT_CDS_TIME = 
        COMPOUND("Short_CDS_Time",
             MEMBER("Day", USHORT),
             MEMBER("Millisec_In_Day", UINT)
        );
    
    public static final CompoundType GRH =
        COMPOUND("Generic_Record_Header",
             MEMBER("Record_Class", UBYTE),
             MEMBER("Instrument_Group", UBYTE),
             MEMBER("Record_Subclass", UBYTE),
             MEMBER("Record_Subclass_Version", UBYTE),
             MEMBER("Record_Size", UINT),
             MEMBER("Record_Start_Time", SHORT_CDS_TIME),
             MEMBER("Record_End_Time", SHORT_CDS_TIME)
        );
    
    public static final CompoundType POINTER =
        COMPOUND("Generic_Record_Pointer",
             MEMBER("Target_Record_Class", UBYTE),
             MEMBER("Target_Instrument_Group", UBYTE),
             MEMBER("Target_Record_Subclass", UBYTE),
             MEMBER("Taregt_Record_Offset", UINT)
        );
    
    public static EpsBasisFormats getInstance() {
        return INSTANCE;
    }
    
    public DataFormat getFormat() {
        return format;
    }
    private static final EpsBasisFormats INSTANCE = new EpsBasisFormats();
    private DataFormat format;
    
    private EpsBasisFormats() {
        format = new DataFormat();
        format.setName("EPS Basis Types");
        format.addTypeDef("short_cds_time", SHORT_CDS_TIME);
        format.addTypeDef("time", SHORT_CDS_TIME);
        format.addTypeDef("grh", GRH);
        format.addTypeDef("pointer", POINTER);
        
    }
}
