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

import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.glayer.Layer;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.eumetsat.metop.sounder.AvhrrOverlay;
import org.eumetsat.metop.sounder.SounderFile;
import org.eumetsat.metop.sounder.SounderOverlay;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;


public class AmsuFile extends SounderFile {
    
    static final int PRODUCT_WIDTH = 30;
    public static final String PRODUCT_TYPE = "AMSU-A";

    public AmsuFile(File file, DataFormat format) throws IOException {
        super(file, format);
    }
    
    @Override
    public Product createProductImpl(ProductReader reader) throws IOException {
        Product product = createProduct(PRODUCT_TYPE, PRODUCT_WIDTH, reader);
        for (AmsuBandInfo bandInfo : AmsuBandInfo.values()) {
            addBand(product, bandInfo);
        }
        addGeocoding(product, AmsuBandInfo.LAT.getName(), AmsuBandInfo.LON.getName());
        return product;
    }
    
    @Override
    public boolean hasOverlayFor(Product avhrrProduct) {
        // TODO check for date
        return true;
    }
    
    @Override
    public AvhrrOverlay createOverlay(Product avhrrProduct) {
        // TODO check for date
        return new AmsuSounderOverlay(this, avhrrProduct);
    }

    @Override
    public Layer createLayer(AvhrrOverlay overlay) {
        if (overlay instanceof AmsuSounderOverlay) {
            AmsuSounderOverlay amsuSounderOverlay = (AmsuSounderOverlay) overlay;
            try {
                return new AmsuSounderLayer(amsuSounderOverlay);
            } catch (IOException e) {
                // ignore
            }
        }
        return null;
    }
    
    public static class NameFilter implements FilenameFilter {

        private final String iasiFilenamePrefix;

        public NameFilter(String avhrrFilename) {
            iasiFilenamePrefix = "AMSA" + avhrrFilename.substring(4, 15);
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith(iasiFilenamePrefix) && name.endsWith(".nat");
        }
    }
}
