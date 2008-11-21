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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public EpsXml(URI uri) throws IOException {
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
    
    private void parseDocument() throws IOException {
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
        parseRecords(element);
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
    
    private void parseRecords(Element element) {
        Element product = element.getChild("product");
        List records = product.getChildren();
        for (int i = 0; i < records.size(); i++) {
            Element elem = (Element) records.get(i);
            List attributes = elem.getAttributes();
            System.out.println(elem);
            System.out.println(attributes);
        }
    }
}
