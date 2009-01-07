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
package org.eumetsat.metop.eps;

import java.net.URI;
import java.net.URL;

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;

import org.eumetsat.metop.eps.EpsMetatData;
import org.eumetsat.metop.eps.EpsXml;

import junit.framework.TestCase;

public class EpsXmlTest extends TestCase {
    
    private EpsXml epsXml;

    @Override
    protected void setUp() throws Exception {
        URL resource = getClass().getResource("eps_iasil1c_6.6.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        epsXml = new EpsXml(uri);
    }
    
    public void testDescription() throws Exception {
        assertEquals("EPS IASI Level 1C Format", epsXml.getFormatDescription());
    }
    
    public void testParameters() throws Exception {
        assertEquals("100", epsXml.getParameter("AMCO"));
        assertEquals("8700", epsXml.getParameter("SS"));
        assertEquals("1", epsXml.getParameter("VP"));
    }
    
    public void testMphr() throws Exception {
        Type mphr = epsXml.getFormat().getTypeDef("mphr");
        assertNotNull(mphr);
        assertTrue(mphr.isCompoundType());
        CompoundType compoundType = (CompoundType) mphr;
        int memberCount = compoundType.getMemberCount();
        assertEquals(72, memberCount);
    }
    
    public void testMphrMetadata() throws Exception {
        Type mphr = epsXml.getFormat().getTypeDef("mphr");
        CompoundType compoundType = (CompoundType) mphr;
        CompoundMember member0 = compoundType.getMember(0);
        
        assertEquals("PRODUCT_NAME", member0.getName());
        Object metadata = member0.getMetadata();
        assertNotNull(metadata);
        assertTrue(metadata instanceof EpsMetatData);
        EpsMetatData epsMetatData = (EpsMetatData) metadata;
        assertEquals("Complete name of the product", epsMetatData.getDescription());
    }
    
    public void testMdr() throws Exception {
        Type mdr = epsXml.getFormat().getTypeDef("mdr:iasi:2");
        assertNotNull(mdr);
        assertTrue(mdr.isCompoundType());
        CompoundType compoundType = (CompoundType) mdr;
        int memberCount = compoundType.getMemberCount();
        assertEquals(53, memberCount);
        assertEquals(27, compoundType.getMemberIndex("GGeoSondLoc"));
        CompoundMember member = compoundType.getMember(27);
        assertNotNull(member);
        assertEquals("GGeoSondLoc", member.getName());
        
        Type memberType = member.getType();
        assertNotNull(memberType);
        assertTrue(memberType.isSequenceType());
        assertEquals("int[2][4][30]", memberType.getName());
        SequenceType sequenceType = (SequenceType) memberType;
        assertEquals(30, sequenceType.getElementCount());
        
        Type elementType = sequenceType.getElementType();
        assertNotNull(elementType);
        assertTrue(elementType.isSequenceType());
        assertEquals("int[2][4]", elementType.getName());
        sequenceType = (SequenceType) elementType;
        assertEquals(4, sequenceType.getElementCount());
        
        elementType = sequenceType.getElementType();
        assertNotNull(elementType);
        assertTrue(elementType.isSequenceType());
        assertEquals("int[2]", elementType.getName());
        sequenceType = (SequenceType) elementType;
        assertEquals(2, sequenceType.getElementCount());
        
        elementType = sequenceType.getElementType();
        assertNotNull(elementType);
        assertTrue(elementType.isSimpleType());
        assertEquals("int", elementType.getName());
        assertSame(SimpleType.INT, elementType);
    }
}
