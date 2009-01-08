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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.util.math.MathUtils;
import org.eumetsat.metop.visat.AvhrrOverlay;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.Raster;


public class AmsuAvhrrOverlay extends AvhrrOverlay {

    private final Product avhrrProduct;
    private final Product amsuProduct;
    private final AmsuIfov[] ifovs;
    private String bandName;
    private ColorPaletteDef paletteDef;
    private Color[] colorPalette;
    
    public AmsuAvhrrOverlay(Product avhrrProduct, Product amsuProduct, String bandName) {
        this.avhrrProduct = avhrrProduct;
        this.amsuProduct = amsuProduct;
        this.bandName = bandName;
        ifovs = readIfovs();
    }
    
    private AmsuIfov[] readIfovs() {
        Raster values = amsuProduct.getBand(bandName).getSourceImage().getData();
        Raster latitudes = amsuProduct.getBand(BandInfo.LAT.name).getGeophysicalImage().getData();
        Raster longitudes = amsuProduct.getBand(BandInfo.LON.name).getGeophysicalImage().getData();
        GeoCoding geoCoding = avhrrProduct.getGeoCoding();
        
        final int width = amsuProduct.getSceneRasterWidth();
        final int height = amsuProduct.getSceneRasterHeight();
        AmsuIfov[] allIfovs = new AmsuIfov[width * height];
        
        int valueMin = Integer.MAX_VALUE;
        int valueMax = 0;
        int index = 0;
        GeoPos amsuGeoPos = new GeoPos();
        PixelPos avhrrPixelPos = new PixelPos();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                amsuGeoPos.lat = latitudes.getSampleFloat(x, y, 0);
                amsuGeoPos.lon = longitudes.getSampleFloat(x, y, 0);
                geoCoding.getPixelPos(amsuGeoPos, avhrrPixelPos);
                Shape shape = new Ellipse2D.Float(avhrrPixelPos.x - 0.5f * 47,
                                                  avhrrPixelPos.y - 0.5f * 47,
                                                      47, 47);
                allIfovs[index] = new AmsuIfov(y, x, shape);
                final int value = values.getSample(x, y, 0);
                allIfovs[index].rawValue = value;
                valueMin = Math.min(valueMin, value);
                valueMax = Math.max(valueMax, value);
                index++;
            }
        }
        paletteDef = new ColorPaletteDef(valueMin, valueMax);
        colorPalette = paletteDef.createColorPalette(Scaling.IDENTITY);
        
        return allIfovs;
    }
    
    public AmsuIfov[] getIfovs() {
        return ifovs;
    }

    public Color getColor(double sample) {
        int numColors = colorPalette.length;
        double min = paletteDef.getMinDisplaySample();
        double max = paletteDef.getMaxDisplaySample();
        int index = MathUtils.floorAndCrop((sample - min) * (numColors - 1.0) / (max - min), 0, numColors-1);
        return colorPalette[index];
    }

}
