/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.eumetsat.metop.visat;

import junit.framework.TestCase;

public class BlackBodyTest extends TestCase {

    public void testTemperatureAtWavenumber() {
        final double k = 64500.0; // wavenumber (m-1)
        final double i = 4.0E-04; // radiance (W/m2/sr/m-1)

        assertEquals(211.24, BlackBody.temperatureAtWavenumber(k, i), 1.0E-02);
    }

    public void testTemperatureAtFrequency() {
        final double f = 23.0; // frequency (GHz)
        final double i = 1.0E-04; // radiance (W/m2/sr/m-1)

        assertEquals(349.24, BlackBody.temperatureAtFrequency(f, i), 1.0E-02);
    }
}
