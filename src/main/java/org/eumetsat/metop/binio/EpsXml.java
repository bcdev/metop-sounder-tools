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
import com.bc.ceres.binio.Format;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SequenceTypeMapper;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.CompoundType.Member;
import com.bc.ceres.binio.util.SequenceElementCountResolver;

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
    private String description;
    private Format format;

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
        map.put("uinteger8", SimpleType.LONG); // TODO how handle this ? (mz, 25.11.2008)

        map.put("vbyte", createVType("vbyte", SimpleType.BYTE));
        map.put("vubyte", createVType("vbyte", SimpleType.UBYTE));
        map.put("vinteger1", createVType("vbyte", SimpleType.BYTE));
        map.put("vuinteger1", createVType("vbyte", SimpleType.UBYTE));
        map.put("vinteger2", createVType("vbyte", SimpleType.SHORT));
        map.put("vuinteger2", createVType("vbyte", SimpleType.USHORT));
        map.put("vinteger4", createVType("vbyte", SimpleType.INT));
        map.put("vuinteger4", createVType("vbyte", SimpleType.UINT));
        map.put("vinteger8", createVType("vbyte", SimpleType.LONG));
        map.put("vuinteger8", createVType("vbyte", SimpleType.LONG)); // TODO how handle this ? (mz, 25.11.2008)
        
        map.put("time", new SequenceType(SimpleType.BYTE, 6));
        return map;
    }
    
    private static Type createVType(String name, SimpleType type) {
        Member[] members = new Member[2];
        members[0] = new CompoundType.Member("scale", SimpleType.BYTE);
        members[1] = new CompoundType.Member("value", type);
        return new CompoundType(name, members);
    }

    public URI getUri() {
        return uri;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Format getFormat() {
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
        List<Member> memberList = new ArrayList<Member>(100);
        Type mphr = epsRecordTypes.get("mphr");
        memberList.add(new CompoundType.Member("mphr", createRecord("mphr", mphr)));
        if (epsRecordTypes.containsKey("sphr")) {
            Type sphr = epsRecordTypes.get("sphr");
            memberList.add(new CompoundType.Member("sphr", createRecord("sphr", sphr)));
        }
        SequenceType sequenceType = new SequenceType(createRecord("ipr", MetopFormats.POINTER));
        Member iprs = new CompoundType.Member("iprs", sequenceType);
        memberList.add(iprs);
        
        //TODO add remaining records
        
        Member[] allRecords = memberList.toArray(new Member[memberList.size()]);
        format = new Format(new CompoundType("all", allRecords));
        final SequenceTypeMapper iprsCountResolver = new SequenceElementCountResolver() {
            
            @Override
            public int getElementCount(CollectionData collectionData, SequenceType sequenceType) throws IOException {
                SequenceData sequence = collectionData.getCompound(0).getCompound(1).getCompound("TOTAL_IPR").getSequence("value");
                byte[] data = new byte[(int) sequence.getSize()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = sequence.getByte(i);
                }
                String string = new String(data).trim();
                Integer value = Integer.valueOf(string);
                return value;
            }
        };
        format.addSequenceTypeMapper(iprs, iprsCountResolver);
    }


    private void parseDescription(Element element) {
        Element child = element.getChild("brief-description");
        description = child.getValue();
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
    
    private Type parseAsciiRecord(Element element) throws DataConversionException {
        List<Member> memberList = new ArrayList<Member>(100);
        List<Element> fields = element.getChildren("field");
        for (int i = 0; i < fields.size(); i++) {
            Element elem = fields.get(i);
            Type type = createAsciiField(elem);
            memberList.add(new CompoundType.Member(type.getName(), type));
        }
        return createCompoundType(element.getName(), memberList);
    }
    
    private Type parseBinaryRecord(Element element) throws DataConversionException {
        List<Member> memberList = new ArrayList<Member>(100);
        String name = element.getName();
        List<Element> fields = element.getChildren();
        for (int i = 0; i < fields.size(); i++) {
            Element part = fields.get(i);
            Type type = createBinaryType(part);
            if (type != null) {
                String elementName = getElementName(part);
                memberList.add(new CompoundType.Member(elementName, type));
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
                type = new SequenceType(SimpleType.BYTE, length/8);
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
        return new SequenceType(elemType, length);
    }

    private Type createCompoundType(String name, List<Member> memberList) {
        Member[] allMembers = memberList.toArray(new Member[memberList.size()]);
        return new CompoundType(name, allMembers);
    }
    
    private CompoundType createRecord(String name, Type bodytype) {
        Member[] members = new Member[2];
        members[0] = new CompoundType.Member("header", MetopFormats.REC_HEAD);
        members[1] = new CompoundType.Member("body", bodytype);
        return new CompoundType(name, members);
    }
    
    private Type createAsciiField(Element elem) throws DataConversionException {
        String name = getElementName(elem);
        int length = getElementLength(elem);
        Member[] members = new Member[3];
        members[0] = new Member("name", new SequenceType(SimpleType.BYTE, 32)); 
        members[1] = new Member("value", new SequenceType(SimpleType.BYTE, length)); 
        members[2] = new Member("cr", SimpleType.BYTE); 
        return new CompoundType(name, members);
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
