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

import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.CompoundMember;

import static com.bc.ceres.binio.TypeBuilder.*;


public class EpsXml {
    private final Map<String, String> parameterMap;
    private final DataFormat format;
    private String formatDescription = "";
    private String productName;

    public EpsXml(URI uri) throws IOException, DataConversionException {
        parameterMap = new HashMap<String, String>(42);
        format = new DataFormat();
        format.setBasisFormat(EpsBasisTypes.getInstance().getFormat());
        parseDocument(uri);
        EpsTypeBuilder epsTypeBuilder = new EpsTypeBuilder("EPS-METOP-Format", format);
        if (productName.equals("MPHR")) {
            format.setType(epsTypeBuilder.buildMPHR());
        } else {
            format.setType(epsTypeBuilder.buildProduct());
        }
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

    private void parseDocument(URI uri) throws IOException, DataConversionException {
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
    
    private void parseDescription(Element element) {
        Element child = element.getChild("brief-description");
        if (child != null) {
            formatDescription = child.getValue();
        }
    }
    
    private void parseParameters(Element element) {
        Element parameters = element.getChild("parameters");
        if (parameters != null) {
            List<Element> children = parameters.getChildren();
            for (int i = 0; i < children.size(); i++) {
                Element elem = children.get(i);
                String name = elem.getAttributeValue("name");
                String value = elem.getAttributeValue("value");
                parameterMap.put(name, value.trim());
            }
        }
    }
    
    private void parseProduct(Element element) throws DataConversionException {
        Element product = element.getChild("product");
        productName = product.getAttribute("name").getValue();
        List<Element> records = product.getChildren();
        for (int i = 0; i < records.size(); i++) {
            Element record = records.get(i);
            String recordName = record.getName();
            Type recordType;
            if (recordName.startsWith("mphr") || recordName.startsWith("sphr")) {
                recordType = parseAsciiRecord(record);
                format.addTypeDef(recordName, recordType);
            } else {
                recordType = parseBinaryRecord(record);
                String instrument = InstrumentGroup.GENERIC.toString();
                Attribute instrumentAttribute = record.getAttribute("instrument");
                if (instrumentAttribute != null) {
                    instrument = instrumentAttribute.getValue();
                }
                String subclass = record.getAttribute("subclass").getValue();
                recordName = EpsBasisTypes.buildTypeName(recordName, instrument, subclass);
                format.addTypeDef(recordName, recordType);
            }
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
        if (format.isTypeDef(typeName)) {
            return format.getTypeDef(typeName);
        }
        if (typeName.equals("bitfield")) {
            final int length = getElementLength(element);
            return  SEQUENCE(SimpleType.BYTE, length/8);
        } else {
            throw new IllegalStateException("unsupported type: "+typeName);
        }
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
        if (typeString.equals("enumerated")) {
            List<Element> items = element.getChildren("item");
            Map<String, String> itemMap = new HashMap<String, String>(items.size());
            for (Element elem : items) {
                String key = elem.getAttribute("value").getValue();
                Attribute nameAttribute = elem.getAttribute("name");
                Attribute descAtribute = elem.getAttribute("description");
                String desc;
                if (nameAttribute != null && descAtribute != null) {
                    desc = nameAttribute.getValue() + " (" + descAtribute.getValue() + ")";
                } else if (nameAttribute != null) {
                    desc = nameAttribute.getValue();
                } else if (descAtribute != null) {
                    desc = descAtribute.getValue();
                } else {
                    desc = "";
                }
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
