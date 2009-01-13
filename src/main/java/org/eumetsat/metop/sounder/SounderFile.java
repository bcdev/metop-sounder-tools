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

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.eumetsat.metop.eps.EpsFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.core.ProgressMonitor;


public abstract class SounderFile extends EpsFile {

    private Map<Band, MdrReader> mdrReaders;

    protected SounderFile(File file, DataFormat format) throws IOException {
        super(file, format);
        mdrReaders = new HashMap<Band, MdrReader>(64);
    }
    
    protected Product createProduct(String productType, int productWidth, ProductReader productReader) throws IOException {
        Product product = new Product(getProductName(), productType, productWidth, getMdrCount(), productReader);
        addMetaData(product);
        return product;
    }
    
    protected void addBand(Product product, BandInfo bandInfo) throws IOException {
        Band band = product.addBand(bandInfo.getName(), bandInfo.getType());
        if (bandInfo.isScaled()) {
            band.setScalingFactor(bandInfo.getScaleFactor());
        }
        mdrReaders.put(band, bandInfo.getReader());
    }
    
    protected void addGeocoding(Product product, String latBand, String lonBand) throws IOException {
        GeoCoding geoCoding = new PixelGeoCoding(product.getBand(latBand), product.getBand(lonBand), null, 5, ProgressMonitor.NULL);
        product.setGeoCoding(geoCoding);
    }
    
    @Override
    public void readBandData(int x, int y, int width, int height, Band band, ProductData buffer, ProgressMonitor pm) throws IOException {
        MdrReader reader = mdrReaders.get(band);
        int bufferIndex = 0;
        SequenceData mdrData = getMdrData();
        pm.beginTask("reading...", height);
        try {
            for (int yi = y; yi < y + height; yi++) {
                CompoundData mdr = mdrData.getCompound(yi).getCompound(1);
                bufferIndex = reader.read(x, width, buffer, bufferIndex, mdr);
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

}
