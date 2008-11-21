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
}
