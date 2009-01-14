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

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.eumetsat.metop.amsu.AmsuSounderLayer;
import org.eumetsat.metop.sounder.SounderFile;
import org.eumetsat.metop.sounder.SounderOverlay;

import java.io.File;
import java.io.IOException;

import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.glayer.Layer;


public class MhsFile extends SounderFile {

    public MhsFile(File file, DataFormat format) throws IOException {
        super(file, format);
    }
    
    @Override
    public Product createProductImpl(ProductReader reader) throws IOException {
        Product product = createProduct("MHS", 90, reader);
        for (MhsBandInfo bandInfo : MhsBandInfo.values()) {
            addBand(product, bandInfo);
        }
        addGeocoding(product, MhsBandInfo.LAT.getName(), MhsBandInfo.LON.getName());
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
        return new SounderOverlay(avhrrProduct, getProduct(), MhsBandInfo.LAT.getName(), MhsBandInfo.LON.getName(), 15.88f/1.1f);
    }
    
    @Override
    public Layer createLayer(SounderOverlay overlay) {
        return new AmsuSounderLayer(overlay);
    }
}
