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

import org.esa.beam.framework.datamodel.*;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.sounder.AbstractSounderOverlay;
import org.eumetsat.metop.sounder.Ifov;
import org.eumetsat.metop.sounder.SounderShapeScaleComputer;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.IOException;


public class AmsuSounderOverlay extends AbstractSounderOverlay {

    private static final float ifovSize = 47.63f;

    public AmsuSounderOverlay(AmsuFile amsufile, Product avhrrProduct) {
        super(amsufile, avhrrProduct);
    }

    @Override
    protected Ifov[] readIfovs() throws IOException {
        EpsFile amsufile = getEpsFile();
        final int height = amsufile.getMdrCount();
        final int width = AmsuFile.PRODUCT_WIDTH;
        final double scalingFactor = AmsuBandInfo.LAT.getScaleFactor();
        ProductData latitudes = amsufile.readData(AmsuBandInfo.LAT, height, width);
        ProductData longitudes = amsufile.readData(AmsuBandInfo.LON, height, width);
        GeoCoding geoCoding = getAvhrrProduct().getGeoCoding();
        Ifov[] allIfovs = new Ifov[width * height];

        SounderShapeScaleComputer scaleComputer = new SounderShapeScaleComputer(amsufile,
                                                                                width,
                                                                                AmsuBandInfo.LAT,
                                                                                AmsuBandInfo.LON,
                                                                                AmsuBandInfo.VZA
        );
        double[] shapeScale = scaleComputer.getIfovShapeScale();
        int index = 0;
        GeoPos amsuGeoPos = new GeoPos();
        PixelPos avhrrPixelPos = new PixelPos();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                amsuGeoPos.lat = (float) (latitudes.getElemIntAt(index) * scalingFactor);
                amsuGeoPos.lon = (float) (longitudes.getElemIntAt(index) * scalingFactor);
                geoCoding.getPixelPos(amsuGeoPos, avhrrPixelPos);
                final float yScale = (float) shapeScale[x];
                Shape shape = new Ellipse2D.Float(avhrrPixelPos.x - 0.5f * ifovSize,
                                                  avhrrPixelPos.y - 0.5f * ifovSize * yScale,
                                                  ifovSize, ifovSize * yScale);
                allIfovs[index] = new AmsuIfov(x, y, shape);
                index++;
            }
        }
        return allIfovs;
    }

    private static class AmsuIfov implements Ifov {
        private final int y;
        private final int x;
        private final Shape shape;

        private AmsuIfov(int x, int y, Shape shape) {
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
            return y * AmsuFile.PRODUCT_WIDTH + x;
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
