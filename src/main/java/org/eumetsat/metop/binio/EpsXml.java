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

import org.esa.beam.visat.actions.ComputeRoiAreaAction;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private String description;
    private Format format;

    public EpsXml(URI uri) throws IOException, DataConversionException {
        this.uri = uri;
        parameterMap = new HashMap<String, String>(42);
        parseDocument();
    }
    
    public URI getUri() {
        return uri;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getParameter(String name) {
        return parameterMap.get(name);
    }
    
    public Format getFormat() {
        return format;
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
        parseFormat(element);
    }

    private void parseDescription(Element element) {
        Element child = element.getChild("brief-description");
        description = child.getValue();
    }
    
    private void parseParameters(Element element) {
        Element parameters = element.getChild("parameters");
        List children = parameters.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Element elem = (Element) children.get(i);
            String name = elem.getAttributeValue("name");
            String value = elem.getAttributeValue("value");
            parameterMap.put(name, value);
        }
    }
    
    private void parseFormat(Element element) throws DataConversionException {
        Element product = element.getChild("product");
        List<Member> memberList = new ArrayList<Member>(100);
        List records = product.getChildren();
        boolean pointerRecordsInserted = false;
        CompoundType.Member iprs = null;
        for (int i = 0; i < records.size(); i++) {
            Element record = (Element) records.get(i);
            String name = record.getName();
            if (name.equals("mphr") || name.equals("sphr")) {
                CompoundType asciiRecord = parseAsciiRecord(record);
                memberList.add(new CompoundType.Member(record.getName(), asciiRecord));
            } else {
                if (!pointerRecordsInserted) {
                    SequenceType sequenceType = new SequenceType(createRecord("ipr", MetopFormats.POINTER));
                    iprs = new CompoundType.Member("iprs", sequenceType);
                    memberList.add(iprs);
                    pointerRecordsInserted = true;
                } else {
                    
                }
            }
        }
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
    
    private CompoundType parseAsciiRecord(Element element) throws DataConversionException {
        List<Member> memberList = new ArrayList<Member>(100);
        List fields = element.getChildren("field");
        for (int i = 0; i < fields.size(); i++) {
            Element elem = (Element) fields.get(i);
            Type type = createAsciiField(elem);
            memberList.add(new CompoundType.Member(type.getName(), type));
        }
        Type type = createCompoundType(element.getName(), memberList);
        return createRecord(element.getName(), type);
    }
    
    private CompoundType parseRecord(Element element) throws DataConversionException {
        List<Member> memberList = new ArrayList<Member>(100);
        boolean isAscii = false;
        String name = element.getName();
        List fields = element.getChildren("field");
        for (int i = 0; i < fields.size(); i++) {
            Element elem = (Element) fields.get(i);
            Type type;
            if (isAscii) {
                type = createAsciiField(elem);
                memberList.add(new CompoundType.Member(elem.getName(), type));
            } else {
//                type = createType(elem);
            }
        }
        Type type = createCompoundType(element.getName(), memberList);
        return createRecord(element.getName(), type);
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
//        String type = elem.getAttribute("type").getValue();
        String name = elem.getAttribute("name").getValue();
        long length = elem.getAttribute("length").getLongValue();
        Member[] members = new Member[3];
        members[0] = new Member("name", new SequenceType(SimpleType.BYTE, 32)); 
        members[1] = new Member("value", new SequenceType(SimpleType.BYTE, (int)length)); 
        members[2] = new Member("cr", SimpleType.BYTE); 
        return new CompoundType(name, members);
    }
    private Type createType(Element elem) throws DataConversionException {
        String type = elem.getAttribute("type").getValue();
        String name = elem.getAttribute("name").getValue();
        Type resultType = null;
        if (type.equals("string") ||
                type.equals("enumerated") ||
                type.equals("time") ||
                type.equals("uinteger")) {
            long length = elem.getAttribute("length").getLongValue();
            Member[] members = new Member[3];
            members[0] = new Member("name", new SequenceType(SimpleType.BYTE, 32)); 
            members[1] = new Member("value", new SequenceType(SimpleType.BYTE, (int)length)); 
            members[2] = new Member("cr", SimpleType.BYTE); 
            resultType = new CompoundType(name, members);
        }
        return resultType;
    }


}
