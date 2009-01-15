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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.eumetsat.metop.sounder.SounderIfov;
import org.eumetsat.metop.sounder.SounderOverlay;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.IOException;


public class MhsSounderOverlay extends SounderOverlay {

    private static final float ifovSize = 15.88f/1.1f;
    private final MhsFile mhsFile;
    private SounderIfov[] ifovs;
    
    public MhsSounderOverlay(MhsFile mhsFile, Product avhrrProduct) {
        super(mhsFile, avhrrProduct);
        this.mhsFile = mhsFile;
    }
    
    public MhsFile getMhsFile() {
        return mhsFile;
    }

    private SounderIfov[] readIfovs() throws IOException {
        final int height = mhsFile.getMdrCount();
        final int width = MhsFile.PRODUCT_WIDTH;
        final float scalingFactor = 1E-4f;
        ProductData latitudes = mhsFile.readData(MhsBandInfo.LAT, height, width);
        ProductData longitudes = mhsFile.readData(MhsBandInfo.LON, height, width);
        
        GeoCoding geoCoding = getAvhrrProduct().getGeoCoding();
        SounderIfov[] allIfovs = new SounderIfov[width * height];
        
        int index = 0;
        GeoPos geoPos = new GeoPos();
        PixelPos avhrrPixelPos = new PixelPos();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                geoPos.lat = latitudes.getElemIntAt(index) * scalingFactor;
                geoPos.lon = longitudes.getElemIntAt(index) * scalingFactor;
                geoCoding.getPixelPos(geoPos, avhrrPixelPos);
                Shape shape = new Ellipse2D.Float(avhrrPixelPos.x - 0.5f * ifovSize,
                                                  avhrrPixelPos.y - 0.5f * ifovSize,
                                                      ifovSize, ifovSize);
                allIfovs[index] = new SounderIfov(y, x, shape);
                index++;
            }
        }
        return allIfovs;
    }

    @Override
    public synchronized SounderIfov[] getIfovs() {
        if (ifovs == null) {
            try {
                ifovs = readIfovs();
                setSelectedIfov(ifovs[0]);
            } catch (IOException e) {
                ifovs = new SounderIfov[0];
            }
        }
        return ifovs;
    }
}
