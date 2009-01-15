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
package org.eumetsat.metop.sounder;

/**
 * Constants for sounder data products.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SounderConstants {
    /**
     * Scaling factor used for (reading) angular relation data.
     */
    public static final double ANGULAR_RELATION_SCALING_FACTOR = 1.0E-2;
    /**
     * Scaling factor used for (reading) earth location data.
     */
    public static final double EARTH_LOCATION_SCALING_FACTOR = 1.0E-4;
    /**
     * Scaling factor used for (reading) scene radiance data.
     */
    public static final double SCENE_RADIANCE_SCALING_FACTOR = 1.0E-7;

    private SounderConstants() {
    }
}
