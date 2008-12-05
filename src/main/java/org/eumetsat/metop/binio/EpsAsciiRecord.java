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
import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceData;

public class EpsAsciiRecord {

    private final CompoundData recordData;

    public EpsAsciiRecord(CompoundData recordData) {
        this.recordData = recordData;
    }
    
    public int getMemberIndex(String memberName) {
        return recordData.getMemberIndex(memberName);
    }
    
    public String getDescription(int memberIndex) {
        EpsAsciiMetatData metaData = getMetaData(memberIndex);
        String description = metaData.getDescription();
        return description != null ? description : "";
    }
     
    public String getUnits(int memberIndex) {
        EpsAsciiMetatData metaData =  getMetaData(memberIndex);
        String units = metaData.getUnits();
        return units != null ? units : "";
    }
    
    public String getString(int memberIndex) throws IOException {
        return getRawString(memberIndex).trim();
    }
    
    public int getInt(int memberIndex) throws IOException {
        ProductData productData = getProductData(memberIndex);
        if (productData.isInt()) {
            return productData.getElemInt();
        }
        throw new IllegalArgumentException("Member ''"+memberIndex+"'' is not of type integer."); 
    }
    
    public ProductData getProductData(int memberIndex) throws IOException {
        String value = getRawString(memberIndex).trim();
        EpsAsciiMetatData metaData = getMetaData(memberIndex);
        String type = metaData.getType();
        String scalingFactor = metaData.getScalingFactor();
        if (type.equals("string")) {
            return ProductData.createInstance(value);
        } else if (type.equals("enumerated")) {
            return ProductData.createInstance(metaData.getItems().get(value));
//        } else if (type.equals("time")) {
//            return ProductData.UTC("converted_time");
//        } else if (type.equals("longtime")) {
//            return "converted_long_time";            
        } else if (type.equals("uinteger") || type.equals("integer")) {
            int intValue = Integer.parseInt(scalingFactor);
            if (scalingFactor != null && !scalingFactor.isEmpty()) {
                int powerIndex = scalingFactor.indexOf('^');
                String scaling = scalingFactor.substring(powerIndex+1);
                int intScale = Integer.parseInt(scaling);
                double doubleValue = intValue / Math.pow(10, intScale);
                return ProductData.createInstance(new double[]{doubleValue});
            } else {
                return ProductData.createInstance(new int[]{intValue});
            }                
        }
        return null;
    }
    
    public String getRawString(int memberIndex) throws IOException {
        CompoundData fieldData = recordData.getCompound(memberIndex);
        SequenceData valueSequence = fieldData.getSequence("value");
        byte[] data = new byte[valueSequence.getElementCount()];
        for (int i = 0; i < data.length; i++) {
            data[i] = valueSequence.getByte(i);
        }
        return new String(data);
    }
    
    private EpsAsciiMetatData getMetaData(int memberIndex) {
        CompoundType compoundType = recordData.getCompoundType();
        CompoundMember member = compoundType.getMember(memberIndex);
        Object object =  member.getMetadata();
        if (object != null && object instanceof EpsAsciiMetatData) {
            return (EpsAsciiMetatData) object;
        } else {
            return null;
        }   
    }
}
