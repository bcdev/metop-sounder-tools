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

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.eumetsat.metop.sounder.SounderFile;
import org.eumetsat.metop.visat.SounderOverlay;

import java.io.File;
import java.io.IOException;

import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.glayer.Layer;


public class AmsuFile extends SounderFile {
    
    private static final int PRODUCT_WIDTH = 30;
    public static final String PRODUCT_TYPE = "AMSU-A";

    public AmsuFile(File file, DataFormat format) throws IOException {
        super(file, format);
    }
    
    @Override
    public synchronized Product createProduct(ProductReader reader) throws IOException {
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
    public SounderOverlay createOverlay(Product avhrrProduct) {
        // TODO check for date
        return new AmsuSounderOverlay(avhrrProduct, getProduct());
    }
    
    @Override
    public Layer createLayer(SounderOverlay overlay) {
        if (overlay instanceof AmsuSounderOverlay) {
            AmsuSounderOverlay amsuSounderOverlay = (AmsuSounderOverlay) overlay;
            return new AmsuSounderLayer(amsuSounderOverlay);
        }
        return null;
    }
}
