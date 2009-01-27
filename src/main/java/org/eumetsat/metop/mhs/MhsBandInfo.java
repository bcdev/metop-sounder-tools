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
import org.eumetsat.metop.sounder.*;
import static org.eumetsat.metop.sounder.SounderConstants.SCENE_RADIANCE_SCALING_FACTOR;
import static org.eumetsat.metop.sounder.SounderConstants.EARTH_LOCATION_SCALING_FACTOR;
import static org.eumetsat.metop.sounder.SounderConstants.ANGULAR_RELATION_SCALING_FACTOR;

public enum MhsBandInfo implements BandInfo {

    RADIANCE01("radiance_1", ProductData.TYPE_INT32, SCENE_RADIANCE_SCALING_FACTOR, 89.0, new RadianceReader("SCENE_RADIANCES", 0)),
    RADIANCE02("radiance_2", ProductData.TYPE_INT32, SCENE_RADIANCE_SCALING_FACTOR, 157.0, new RadianceReader("SCENE_RADIANCES", 1)),
    RADIANCE03("radiance_3", ProductData.TYPE_INT32, SCENE_RADIANCE_SCALING_FACTOR, 183.311, new RadianceReader("SCENE_RADIANCES", 2)),
    RADIANCE04("radiance_4", ProductData.TYPE_INT32, SCENE_RADIANCE_SCALING_FACTOR, 183.311, new RadianceReader("SCENE_RADIANCES", 3)),
    RADIANCE05("radiance_5", ProductData.TYPE_INT32, SCENE_RADIANCE_SCALING_FACTOR, 190.311, new RadianceReader("SCENE_RADIANCES", 4)),

    SZA("solar_zenith_angle", ProductData.TYPE_INT16, ANGULAR_RELATION_SCALING_FACTOR, new GeometryReader(0)),
    VZA("view_zenith_angle", ProductData.TYPE_INT16, ANGULAR_RELATION_SCALING_FACTOR, new GeometryReader(1)),
    SAA("solar_azimuth_angle", ProductData.TYPE_INT16, ANGULAR_RELATION_SCALING_FACTOR, new GeometryReader(2)),
    VAA("view_azimuth_angle", ProductData.TYPE_INT16, ANGULAR_RELATION_SCALING_FACTOR, new GeometryReader(3)),

    LAT("latitude", ProductData.TYPE_INT32, EARTH_LOCATION_SCALING_FACTOR, new LocationReader(0)),
    LON("longitude", ProductData.TYPE_INT32, EARTH_LOCATION_SCALING_FACTOR, new LocationReader(1)),

    SURFACE("surfave_type", ProductData.TYPE_INT16, 1.0, new FlagReader("SURFACE_PROPERTIES")),
    ELEVATION("elevation", ProductData.TYPE_INT16, 1.0, new FlagReader("TERRAIN_ELEVATION"));

    private MhsBandInfo(String name, int type, double scaleFactor, MdrReader reader) {
        this(name, type, scaleFactor, 0.0, reader);
    }

    private MhsBandInfo(String name, int type, double scaleFactor, double frequency, MdrReader reader) {
        this.name = name;
        this.type = type;
        this.scaleFactor = scaleFactor;
        this.frequency = frequency;
        this.reader = reader;
    }

    private final String name;
    private final int type;
    private final double scaleFactor;
    private final double frequency;
    private final MdrReader reader;

    @Override
    public boolean isScaled() {
        return scaleFactor != 1.0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public double getScaleFactor() {
        return scaleFactor;
    }

    @Override
    public double getFrequency() {
        return frequency;
    }

    @Override
    public MdrReader getReader() {
        return reader;
    }
}
