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
package org.eumetsat.metop.iasi;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.util.math.MathUtils;
import org.eumetsat.metop.iasi.IasiFile.Geometry;

import java.io.IOException;


class IfovShapeFactory {
    private static final double Re = 6378137;
    private static final double H = 819000;
    
    private final IasiFile iasiFile;
    private final GeoPos nadirGP;
    
    public IfovShapeFactory(IasiFile iasiFile, GeoCoding geoCoding) throws IOException {
        this.iasiFile = iasiFile;
        this.nadirGP = computeNadirGeoPos(geoCoding);
        
        // find first line over AVHRR
        //read all GeoPos for this line
        // read all vza for this line
        //foreach ifov in line
          // compute scale and save
        
    }

    private GeoPos computeNadirGeoPos(GeoCoding geoCoding) throws IOException {
        GeoPos gp1 = iasiFile.readGeoPos(IasiFile.computeIfovId(0, 14, 0));
        GeoPos gp2 = iasiFile.readGeoPos(IasiFile.computeIfovId(0, 15, 3));
        PixelPos pp1 = geoCoding.getPixelPos(gp1, null);
        PixelPos pp2 = geoCoding.getPixelPos(gp2, null);
        PixelPos nadir = new PixelPos((pp1.x+pp2.x)/2, (pp1.y+pp2.y)/2);
        return geoCoding.getGeoPos(nadir, null);
    }
    
    private double getIfovScale(int ifovId) throws IOException {
        GeoPos gp = iasiFile.readGeoPos(ifovId);
        double distance = MathUtils.sphereDistanceDeg(Re, nadirGP.lon, nadirGP.lat, gp.lon, gp.lat);
        Geometry geometry = iasiFile.readGeometry(ifovId);
        
        double theta = MathUtils.DTOR * geometry.vza;
        double alpha = distance / Re;
        double hTheta = Math.sin(alpha) * (H + Re) / Math.sin(theta);
        double scale = hTheta/H;
        
        return scale;
    }
}
