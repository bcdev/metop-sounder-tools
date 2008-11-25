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

import java.net.URI;
import java.net.URL;

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.CompoundType.Member;

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
        assertEquals("EPS IASI Level 1C Format", epsXml.getDescription());
    }
    
    public void testParameters() throws Exception {
        assertEquals("100", epsXml.getParameter("AMCO"));
        assertEquals("8700", epsXml.getParameter("SS"));
        assertEquals("1", epsXml.getParameter("VP"));
    }
    
    public void testMphr() throws Exception {
        Type mphr = epsXml.getEpsRecordType("mphr");
        assertNotNull(mphr);
        assertTrue(mphr.isCompoundType());
        CompoundType compoundType = (CompoundType) mphr;
        int memberCount = compoundType.getMemberCount();
        assertEquals(72, memberCount);
    }
    
    public void testMdr() throws Exception {
        Type mdr = epsXml.getEpsRecordType("mdr");
        assertNotNull(mdr);
        assertTrue(mdr.isCompoundType());
        CompoundType compoundType = (CompoundType) mdr;
        int memberCount = compoundType.getMemberCount();
        assertEquals(53, memberCount);
        assertEquals(27, compoundType.getMemberIndex("GGeoSondLoc"));
        Member member = compoundType.getMember(27);
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
