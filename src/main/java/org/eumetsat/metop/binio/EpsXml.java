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

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.internal.VarElementCountSequenceType;

import static com.bc.ceres.binio.TypeBuilder.*;

/**
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class EpsXml {
    
    private final URI uri;
    private final Map<String, String> parameterMap;
    private final Map<String, Type> epsRecordTypes;
    private final static Map<String, Type> typedefs = epsTypes();
    private String formatDescription;
    private DataFormat format;

    public EpsXml(URI uri) throws IOException, DataConversionException {
        this.uri = uri;
        parameterMap = new HashMap<String, String>(42);
        epsRecordTypes = new HashMap<String, Type>(42);
        parseDocument();
        createFormat();
    }
    
    private static Map<String, Type> epsTypes() {
        HashMap<String,Type> map = new HashMap<String, Type>(42);
        map.put("byte", SimpleType.BYTE);
        map.put("ubyte", SimpleType.UBYTE);
        map.put("enumerated", SimpleType.UBYTE);
        map.put("boolean", SimpleType.BYTE);
        map.put("integer1", SimpleType.BYTE);
        map.put("uinteger1", SimpleType.UBYTE);
        map.put("integer2", SimpleType.SHORT);
        map.put("uinteger2", SimpleType.USHORT);
        map.put("integer4", SimpleType.INT);
        map.put("uinteger4", SimpleType.UINT);
        map.put("integer8", SimpleType.LONG);
        map.put("uinteger8", SimpleType.ULONG);

        map.put("vbyte", createVType("vbyte", SimpleType.BYTE));
        map.put("vubyte", createVType("vbyte", SimpleType.UBYTE));
        map.put("vinteger1", createVType("vbyte", SimpleType.BYTE));
        map.put("vuinteger1", createVType("vbyte", SimpleType.UBYTE));
        map.put("vinteger2", createVType("vbyte", SimpleType.SHORT));
        map.put("vuinteger2", createVType("vbyte", SimpleType.USHORT));
        map.put("vinteger4", createVType("vbyte", SimpleType.INT));
        map.put("vuinteger4", createVType("vbyte", SimpleType.UINT));
        map.put("vinteger8", createVType("vbyte", SimpleType.LONG));
        map.put("vuinteger8", createVType("vbyte", SimpleType.LONG));
        
        map.put("time", SEQUENCE(SimpleType.BYTE, 6));
        return map;
    }
    
    private static Type createVType(String name, SimpleType type) {
        return COMPOUND(name, MEMBER("scale", SimpleType.BYTE), MEMBER("value", type));
    }

    public URI getUri() {
        return uri;
    }
    
    public String getFormatDescription() {
        return formatDescription;
    }
    
    public DataFormat getFormat() {
        return format;
    }
    
    String getParameter(String name) {
        return parameterMap.get(name);
    }

    Type getEpsRecordType(String typeName) {
        return epsRecordTypes.get(typeName);
    }
    
    private void parseDocument() throws IOException, DataConversionException {
        SAXBuilder builder = new SAXBuilder();
        Document document;
        try {
            document = builder.build(uri.toURL());
        } catch (JDOMException e) {
            throw new IOException(MessageFormat.format("Failed to read ''{0}''", uri), e);
        }
        Element element = document.getRootElement();
        parseDescription(element);
        parseParameters(element);
        parseProduct(element);
    }
    
    private void createFormat() {
        List<CompoundMember> memberList = new ArrayList<CompoundMember>(100);
        Type mphr = epsRecordTypes.get("mphr");
        memberList.add(MEMBER("mphr", createRecord("mphr", mphr)));
        if (epsRecordTypes.containsKey("sphr")) {
            Type sphr = epsRecordTypes.get("sphr");
            memberList.add(MEMBER("sphr", createRecord("sphr", sphr)));
        }
        CompoundType recordType = createRecord("ipr", MetopFormats.POINTER);
        CompoundMember iprs = MEMBER("iprs", new MyVarElementCountSequenceType(recordType));
        memberList.add(iprs);
        
        //TODO add remaining records
        
        CompoundMember[] allRecords = memberList.toArray(new CompoundMember[memberList.size()]);
        format = new DataFormat(COMPOUND("all", allRecords));
    }
    
    private static class MyVarElementCountSequenceType extends VarElementCountSequenceType {

        protected MyVarElementCountSequenceType(Type elementType) {
            super(elementType);
        }

        @Override
        protected int resolveElementCount(CollectionData parentData) throws IOException {
            SequenceData sequence = parentData.getCompound(0).getCompound(1).getCompound("TOTAL_IPR").getSequence("value");
          byte[] data = new byte[(int) sequence.getSize()];
          for (int i = 0; i < data.length; i++) {
              data[i] = sequence.getByte(i);
          }
          String string = new String(data).trim();
          Integer value = Integer.valueOf(string);
          return value+1; // to also read by UMARF corrupted products
        }
        
    }

    private void parseDescription(Element element) {
        Element child = element.getChild("brief-description");
        formatDescription = child.getValue();
    }
    
    private void parseParameters(Element element) {
        Element parameters = element.getChild("parameters");
        List<Element> children = parameters.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Element elem = children.get(i);
            String name = elem.getAttributeValue("name");
            String value = elem.getAttributeValue("value");
            parameterMap.put(name, value.trim());
        }
    }
    
    private void parseProduct(Element element) throws DataConversionException {
        Element product = element.getChild("product");
        List<Element> records = product.getChildren();
        for (int i = 0; i < records.size(); i++) {
            Element record = records.get(i);
            String recordName = record.getName();
            Type recordType;
            if (recordName.equals("mphr") || recordName.equals("sphr")) {
                recordType = parseAsciiRecord(record);
            } else {
                recordType = parseBinaryRecord(record);
            }
            epsRecordTypes.put(recordName, recordType);
        }
    }
    
    private Type parseAsciiRecord(Element recordElement) throws DataConversionException {
        List<CompoundMember> memberList = new ArrayList<CompoundMember>(100);
        List<Element> fields = recordElement.getChildren("field");
        for (int i = 0; i < fields.size(); i++) {
            Element fieldElement = fields.get(i);
            Type type = createAsciiField(fieldElement);
            CompoundMember member = MEMBER(type.getName(), type);
            member.setMetadata(getAsciiMetadata(fieldElement));
            memberList.add(member);
        }
        return createCompoundType(recordElement.getName(), memberList);
    }
    
    private Type parseBinaryRecord(Element element) throws DataConversionException {
        List<CompoundMember> memberList = new ArrayList<CompoundMember>(100);
        String name = element.getName();
        List<Element> fields = element.getChildren();
        for (int i = 0; i < fields.size(); i++) {
            Element part = fields.get(i);
            Type type = createBinaryType(part);
            if (type != null) {
                String elementName = getElementName(part);
                memberList.add(MEMBER(elementName, type));
            }
        }
        return createCompoundType(name, memberList);
    }

    private Type createBinaryType(Element part) throws DataConversionException {
        String partName = part.getName();
        if (partName.equals("field")) {
            return createField(part);
        } else if (partName.equals("array")) {
            return createArray(part);
        }
        return null;
    }
    
    private Type createField(Element element) throws DataConversionException {
        String typeName = element.getAttribute("type").getValue();
        Type type = typedefs.get(typeName);
        if (type == null) {
            if (typeName.equals("bitfield")) {
                int length = getElementLength(element);
                type = SEQUENCE(SimpleType.BYTE, length/8);
            } else {
                throw new IllegalStateException("unsupported type: "+typeName);
            }
        }
        return type;
    }
    
    private Type createArray(Element element) throws DataConversionException {
        int length = getElementLength(element);
        List<Element> children = element.getChildren();
        if (children.size() != 1 ) {
            throw new IllegalStateException("array could only contain one element");
        }
        Element child = children.get(0);
        Type elemType = createBinaryType(child);
        return SEQUENCE(elemType, length);
    }

    private Type createCompoundType(String name, List<CompoundMember> memberList) {
        CompoundMember[] allMembers = memberList.toArray(new CompoundMember[memberList.size()]);
        return COMPOUND(name, allMembers);
    }
    
    private CompoundType createRecord(String name, Type bodytype) {
        return COMPOUND(name, MEMBER("header", MetopFormats.REC_HEAD), MEMBER("body", bodytype));
    }
    
    private Type createAsciiField(Element elem) throws DataConversionException {
        String name = getElementName(elem);
        int length = getElementLength(elem);
        return COMPOUND(name, 
                    MEMBER("name", SEQUENCE(SimpleType.BYTE, 32)), 
                    MEMBER("value", SEQUENCE(SimpleType.BYTE, length)),
                    MEMBER("cr", SimpleType.BYTE));
    }
    
    private EpsAsciiMetatData getAsciiMetadata(Element element) {
        EpsAsciiMetatData metaData = new EpsAsciiMetatData();
        Attribute type = element.getAttribute("type");
        String typeString = type.getValue();
        metaData.setType(typeString);
        Attribute description = element.getAttribute("description");
        if (description != null) {
            metaData.setDescription(description.getValue());
        }
        Attribute units = element.getAttribute("units"); // opt
        if (units != null) {
            metaData.setUnits(units.getValue());
        }
        Attribute scalingFactor = element.getAttribute("scaling-factor"); //opt
        if (scalingFactor != null) {
            metaData.setScalingFactor(scalingFactor.getValue());
        }
        if (type.equals("enumerated")) {
            List<Element> items = element.getChildren("item");
            Map<String, String> itemMap = new HashMap<String, String>(items.size());
            for (Element elem : items) {
                String key = elem.getAttribute("value").getValue();
                String desc = elem.getAttribute("description").getValue();
                itemMap.put(key, desc);
            }
            metaData.setItems(itemMap);
        }
        return metaData;
    }
    
    private int getElementLength(Element element) throws DataConversionException{
        Attribute lengthAttribute = element.getAttribute("length");
        String stringValue = lengthAttribute.getValue();
        if (stringValue.startsWith("$")) {
            String parameterName = stringValue.substring(1);
            return Integer.parseInt(parameterMap.get(parameterName));
        } else {
            return lengthAttribute.getIntValue();
        }
    }
    
    private String getElementName(Element element) {
        Attribute nameAttribute = element.getAttribute("name");
        return nameAttribute.getValue();
    }
}
