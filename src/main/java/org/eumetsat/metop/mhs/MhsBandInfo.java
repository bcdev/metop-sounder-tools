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
package org.eumetsat.metop.mhs;

import org.esa.beam.framework.datamodel.ProductData;
import org.eumetsat.metop.sounder.BandInfo;
import org.eumetsat.metop.sounder.FlagReader;
import org.eumetsat.metop.sounder.GeometryReader;
import org.eumetsat.metop.sounder.LocationReader;
import org.eumetsat.metop.sounder.MdrReader;
import org.eumetsat.metop.sounder.RadianceReader;

public enum MhsBandInfo implements BandInfo {
    RADIANCE01("radiance_1", ProductData.TYPE_INT32, 1.0E-7, new RadianceReader("SCENE_RADIANCES", 0)),
    RADIANCE02("radiance_2", ProductData.TYPE_INT32, 1.0E-7, new RadianceReader("SCENE_RADIANCES", 1)),
    RADIANCE03("radiance_3", ProductData.TYPE_INT32, 1.0E-7, new RadianceReader("SCENE_RADIANCES", 2)),
    RADIANCE04("radiance_4", ProductData.TYPE_INT32, 1.0E-7, new RadianceReader("SCENE_RADIANCES", 3)),
    RADIANCE05("radiance_5", ProductData.TYPE_INT32, 1.0E-7, new RadianceReader("SCENE_RADIANCES", 4)),
    
    SZA("solar_zenith_angle", ProductData.TYPE_INT16, 1.0E-2, new GeometryReader(0)),
    VZA("view_zenith_angle", ProductData.TYPE_INT16, 1.0E-2, new GeometryReader(1)),
    SAA("solar_azimuth_angle", ProductData.TYPE_INT16, 1.0E-2, new GeometryReader(2)),
    VAA("view_azimuth_angle", ProductData.TYPE_INT16, 1.0E-2, new GeometryReader(3)),
    
    LAT("latitude", ProductData.TYPE_INT32, 1.0E-4, new LocationReader(0)),
    LON("longitude", ProductData.TYPE_INT32, 1.0E-4, new LocationReader(1)),
    
    SURFACE("surfave_type", ProductData.TYPE_INT16, 1.0, new FlagReader("SURFACE_PROPERTIES")),
    ELEVATION("elevation", ProductData.TYPE_INT16, 1.0, new FlagReader("TERRAIN_ELEVATION"));
    
    private MhsBandInfo(String name, int type, double scaleFactor, MdrReader reader) {
        this.name = name;
        this.type = type;
        this.scaleFactor = scaleFactor;
        this.reader = reader;
    }
    
    private final String name;
    private final int type;
//    final String unit;
//
//    final double scaleOffset;
    private final double scaleFactor;
//    final Number noDataValue;
//    final double min;
//    final double max;
//    final String description;
    private final MdrReader reader;
    
    public boolean isScaled() {
        return scaleFactor != 1.0;
    }
    
    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public MdrReader getReader() {
        return reader;
    }
}
