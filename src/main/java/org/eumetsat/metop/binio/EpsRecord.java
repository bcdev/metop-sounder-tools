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

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceData;

public class EpsRecord {

    private final CompoundData recordData;
    private final boolean isAscii;

    public EpsRecord(CompoundData recordData, boolean isAscii) {
        this.recordData = recordData;
        this.isAscii = isAscii;
    }
    
    public int getMemberIndex(String memberName) {
        return recordData.getMemberIndex(memberName);
    }
    
    public String getDescription(int memberIndex) {
        EpsMetatData metaData = getMetaData(memberIndex);
        String description = metaData.getDescription();
        return description != null ? description : "";
    }
     
    public String getUnits(int memberIndex) {
        EpsMetatData metaData =  getMetaData(memberIndex);
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
        if (isAscii) {
            return getProductDataAscii(memberIndex);
        } else {
            return getProductDataBinary(memberIndex);
        }
    }
    
    public ProductData getProductDataAscii(int memberIndex) throws IOException {
        String valueAsString = getRawString(memberIndex).trim();
        EpsMetatData metaData = getMetaData(memberIndex);
        String type = metaData.getType();
        String scalingFactor = metaData.getScalingFactor();
        if (type.equals("string")) {
            return ProductData.createInstance(valueAsString);
        } else if (type.equals("enumerated")) {
            return ProductData.createInstance(metaData.getItems().get(valueAsString));
        } else if (type.equals("time")) {
            return ProductData.createInstance("converted_time");
        } else if (type.equals("longtime")) {
            return ProductData.createInstance("converted_long_time");            
        } else if (type.equals("integer") || type.equals("uinteger")) {
            long longValue = Long.parseLong(valueAsString);
            if (scalingFactor != null && !scalingFactor.isEmpty()) {
                int powerIndex = scalingFactor.indexOf('^');
                String scaling = scalingFactor.substring(powerIndex+1);
                int intScale = Integer.parseInt(scaling);
                double doubleValue = longValue / Math.pow(10, intScale);
                return ProductData.createInstance(new double[]{doubleValue});
            } else {
                return ProductData.createInstance(new long[]{longValue});
            }                
        }
        return ProductData.createInstance(valueAsString);
    }
    
    public ProductData getProductDataBinary(int memberIndex) throws IOException {
//        String valueAsString = getRawString(memberIndex).trim();
        EpsMetatData metaData = getMetaData(memberIndex);
        String type = metaData.getType();
        String scalingFactor = metaData.getScalingFactor();
        if (type.equals("string")) {
            return ProductData.createInstance(getAsString(recordData.getSequence(memberIndex)));
        } else if (type.equals("enumerated")) {
//            return ProductData.createInstance(metaData.getItems().get(valueAsString));
        } else if (type.equals("time")) {
            return ProductData.createInstance("converted_time");
        } else if (type.equals("longtime")) {
            return ProductData.createInstance("converted_long_time");            
        } else if (type.startsWith("integer") || type.startsWith("uinteger")) {
            int intValue = recordData.getInt(memberIndex);
//            long longValue = Long.parseLong(valueAsString);
            if (scalingFactor != null && !scalingFactor.isEmpty() && !scalingFactor.contains(",")) {
                int powerIndex = scalingFactor.indexOf('^');
                String scaling = scalingFactor.substring(powerIndex+1);
                int intScale = Integer.parseInt(scaling);
                double doubleValue = intValue / Math.pow(10, intScale);
                return ProductData.createInstance(new double[]{doubleValue});
            } else {
                return ProductData.createInstance(new int[]{intValue});
            }                
        }
        return ProductData.createInstance("TODO "+type);
    }
    
    public MetadataElement getAsMetaDataElement() throws IOException {
        CompoundType type = recordData.getCompoundType();
        final int memberCount = type.getMemberCount();
        MetadataElement metadataElement = new MetadataElement(getRecordName());
        for (int i = 0; i < memberCount; i++) {
            String name = type.getMemberName(i);
            ProductData data = getProductData(i);
            MetadataAttribute attribute = new MetadataAttribute(name, data, true);
            attribute.setDescription(getDescription(i));
            attribute.setUnit(getUnits(i));
            metadataElement.addAttribute(attribute);
        }
        return metadataElement;
    }
    
    private String getRecordName() {
        return recordData.getCompoundType().getName();
    }

    private String getAsString(SequenceData valueSequence) throws IOException {
        byte[] data = new byte[valueSequence.getElementCount()];
        for (int i = 0; i < data.length; i++) {
            data[i] = valueSequence.getByte(i);
        }
        return new String(data).trim();
    }
    
    private String getRawString(int memberIndex) throws IOException {
        CompoundData fieldData = recordData.getCompound(memberIndex);
        return getAsString(fieldData.getSequence("value"));
    }
    
    // debug
    void printMemberNames() {
        CompoundMember[] members = recordData.getCompoundType().getMembers();
        for (CompoundMember compoundMember : members) {
            System.out.println(compoundMember.getName());
        }
    }
    
    private EpsMetatData getMetaData(int memberIndex) {
        CompoundType compoundType = recordData.getCompoundType();
        CompoundMember member = compoundType.getMember(memberIndex);
        Object object =  member.getMetadata();
        if (object != null && object instanceof EpsMetatData) {
            return (EpsMetatData) object;
        } else {
            return null;
        }   
    }
}
