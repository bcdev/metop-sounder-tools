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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.eumetsat.metop.visat.SounderOverlay;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.Raster;

import javax.swing.JComponent;
import javax.swing.JLabel;


public class AmsuSounderOverlay implements SounderOverlay {

    private final Product avhrrProduct;
    private final Product amsuProduct;
    private final AmsuIfov[] ifovs;
    
    public AmsuSounderOverlay(Product avhrrProduct, Product amsuProduct) {
        this.avhrrProduct = avhrrProduct;
        this.amsuProduct = amsuProduct;
        ifovs = readIfovs();
    }
    
    public Product getAvhrrProduct() {
        return avhrrProduct;
    }

    public Product getAmsuProduct() {
        return amsuProduct;
    }

    private AmsuIfov[] readIfovs() {
        Raster latitudes = amsuProduct.getBand(BandInfo.LAT.name).getGeophysicalImage().getData();
        Raster longitudes = amsuProduct.getBand(BandInfo.LON.name).getGeophysicalImage().getData();
        GeoCoding geoCoding = avhrrProduct.getGeoCoding();
        
        final int width = amsuProduct.getSceneRasterWidth();
        final int height = amsuProduct.getSceneRasterHeight();
        AmsuIfov[] allIfovs = new AmsuIfov[width * height];
        
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
                index++;
            }
        }
        return allIfovs;
    }
    
    public AmsuIfov[] getIfovs() {
        return ifovs;
    }

    
    public JComponent createInfoComponent() {
        JLabel label = new JLabel("No info available!");
        return label;
    }
}
