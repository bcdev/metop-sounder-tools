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

import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceData;

public class EpsAsciiRecord {

    private final CompoundData recordData;

    public EpsAsciiRecord(CompoundData recordData) {
        this.recordData = recordData;
    }
    
    public String getDescription(int memberIndex) throws IOException {
        CompoundData compoundData = recordData.getCompound(memberIndex);
        EpsAsciiMetatData metaData = getMetaData(compoundData);
        String description = metaData.getDescription();
        return description != null ? description : "";
    }
    
    public String getUnits(int memberIndex) throws IOException {
        CompoundData compoundData = recordData.getCompound(memberIndex);
        EpsAsciiMetatData metaData = getMetaData(compoundData);
        String units = metaData.getUnits();
        return units != null ? units : "";
    }
    
    public ProductData getProductData(String memberName) throws IOException {
        CompoundData fieldData = recordData.getCompound(memberName);
        String value = getRawString(fieldData).trim();
        EpsAsciiMetatData metaData = getMetaData(fieldData);
        String type = metaData.getType();
        if (type.equals("string")) {
            return ProductData.createInstance(value);
        } else if (type.equals("enumerated")) {
            return ProductData.createInstance(metaData.getItems().get(value));
//        } else if (type.equals("time")) {
//            return ProductData.UTC("converted_time");
//        } else if (type.equals("longtime")) {
//            return "converted_long_time";            
//        } else if (type.equals("uinteger")) {
//            return value;            
        }
        return null;
    }
    
    public String getRawString(CompoundData fieldData) throws IOException {
        SequenceData valueSequence = fieldData.getSequence("value");
        byte[] data = new byte[valueSequence.getElementCount()];
        for (int i = 0; i < data.length; i++) {
            data[i] = valueSequence.getByte(i);
        }
        return new String(data);
    }
    
    private EpsAsciiMetatData getMetaData(CompoundData fieldData) {
        CompoundType compoundType = fieldData.getCompoundType();
        Object object = compoundType.getMetadata();
        if (object != null && object instanceof EpsAsciiMetatData) {
            return (EpsAsciiMetatData) object;
        } else {
            return null;
        }   
    }
}
