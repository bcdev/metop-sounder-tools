/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.eumetsat.metop.amsu;

import org.esa.beam.framework.datamodel.ProductData;

enum BandInfo {
    RADIANCE01("radiance_1", ProductData.TYPE_INT32, 1e-7),
    RADIANCE02("radiance_2", ProductData.TYPE_INT32,1e-7);
    
    private BandInfo(String name, int type, double scaleFactor) {
        this.name = name;
        this.type = type;
        this.scaleFactor = scaleFactor;
    }
    
    final String name;
    final int type;
//    final String unit;
//
//    final double scaleOffset;
    final double scaleFactor;
//    final Number noDataValue;
//    final double min;
//    final double max;
//    final String description;
}
