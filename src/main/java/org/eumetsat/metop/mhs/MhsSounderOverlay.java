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
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.sounder.AbstractSounderOverlay;
import org.eumetsat.metop.sounder.SounderShapeScaleComputer;
import org.eumetsat.metop.sounder.Ifov;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.IOException;


public class MhsSounderOverlay extends AbstractSounderOverlay {

    private static final float ifovSize = 15.88f;
    
    public MhsSounderOverlay(MhsFile mhsFile, Product avhrrProduct) {
        super(mhsFile, avhrrProduct);
    }

    @Override
    protected Ifov[] readIfovs() throws IOException {
        EpsFile mhsFile = getEpsFile();
        final int height = mhsFile.getMdrCount();
        final int width = MhsFile.PRODUCT_WIDTH;
        final double scalingFactor = MhsBandInfo.LAT.getScaleFactor();
        ProductData latitudes = mhsFile.readData(MhsBandInfo.LAT, height, width);
        ProductData longitudes = mhsFile.readData(MhsBandInfo.LON, height, width);
        GeoCoding geoCoding = getAvhrrProduct().getGeoCoding();
        Ifov[] allIfovs = new Ifov[width * height];

        SounderShapeScaleComputer scaleComputer = new SounderShapeScaleComputer(mhsFile,
                                                                                width,
                                                                                MhsBandInfo.LAT, 
                                                                                MhsBandInfo.LON,
                                                                                MhsBandInfo.VZA
        );
        double[] shapeScale = scaleComputer.getIfovShapeScale();
        
        int index = 0;
        GeoPos geoPos = new GeoPos();
        PixelPos avhrrPixelPos = new PixelPos();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                geoPos.lat = (float) (latitudes.getElemIntAt(index) * scalingFactor);
                geoPos.lon = (float) (longitudes.getElemIntAt(index) * scalingFactor);
                geoCoding.getPixelPos(geoPos, avhrrPixelPos);
                final float yScale = (float) shapeScale[x];
                Shape shape = new Ellipse2D.Float(avhrrPixelPos.x - 0.5f * ifovSize,
                                                  avhrrPixelPos.y - 0.5f * ifovSize * yScale,
                                                      ifovSize, ifovSize * yScale);
                allIfovs[index] = new MhsIfov(x, y, shape);
                index++;
            }
        }
        return allIfovs;
    }

    private static class MhsIfov implements Ifov {
        private final int y;
        private final int x;
        private final Shape shape;

        private MhsIfov(int x, int y, Shape shape) {
            this.y = y;
            this.x = x;
            this.shape = shape;
        }

        @Override
        public final int getMdrIndex() {
            return y;
        }

        @Override
        public final int getIfovIndex() {
            return y * MhsFile.PRODUCT_WIDTH + x;
        }

        @Override
        public final int getIfovInMdrIndex() {
            return x;
        }

        @Override
        public final Shape getShape() {
            return shape;
        }
    }
}
