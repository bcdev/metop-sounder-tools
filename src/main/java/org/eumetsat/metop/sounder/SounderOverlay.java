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
package org.eumetsat.metop.sounder;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.Raster;

import javax.swing.JComponent;
import javax.swing.JLabel;


public class SounderOverlay {

    private final Product avhrrProduct;
    private final Product amsuProduct;
    private final SounderIfov[] ifovs;
    private final String latBand;
    private final String lonBand;
    private final int ifovSize;
    
    public SounderOverlay(Product avhrrProduct, Product amsuProduct, String latBand, String lonBand, int ifovSize) {
        this.avhrrProduct = avhrrProduct;
        this.amsuProduct = amsuProduct;
        this.latBand = latBand;
        this.lonBand = lonBand;
        this.ifovSize = ifovSize;
        ifovs = readIfovs();
    }
    
    public Product getAvhrrProduct() {
        return avhrrProduct;
    }

    public Product getAmsuProduct() {
        return amsuProduct;
    }

    private SounderIfov[] readIfovs() {
        Raster latitudes = amsuProduct.getBand(latBand).getGeophysicalImage().getData();
        Raster longitudes = amsuProduct.getBand(lonBand).getGeophysicalImage().getData();
        GeoCoding geoCoding = avhrrProduct.getGeoCoding();
        
        final int width = amsuProduct.getSceneRasterWidth();
        final int height = amsuProduct.getSceneRasterHeight();
        SounderIfov[] allIfovs = new SounderIfov[width * height];
        
        int index = 0;
        GeoPos amsuGeoPos = new GeoPos();
        PixelPos avhrrPixelPos = new PixelPos();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                amsuGeoPos.lat = latitudes.getSampleFloat(x, y, 0);
                amsuGeoPos.lon = longitudes.getSampleFloat(x, y, 0);
                geoCoding.getPixelPos(amsuGeoPos, avhrrPixelPos);
                Shape shape = new Ellipse2D.Float(avhrrPixelPos.x - 0.5f * ifovSize,
                                                  avhrrPixelPos.y - 0.5f * ifovSize,
                                                      ifovSize, ifovSize);
                allIfovs[index] = new SounderIfov(y, x, shape);
                index++;
            }
        }
        return allIfovs;
    }
    
    public SounderIfov[] getIfovs() {
        return ifovs;
    }

    
    public JComponent createInfoComponent() {
        JLabel label = new JLabel("No info available!");
        return label;
    }
}
