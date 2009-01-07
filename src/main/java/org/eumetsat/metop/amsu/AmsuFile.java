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

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.core.ProgressMonitor;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.eumetsat.metop.eps.EpsFile;

import java.io.File;
import java.io.IOException;


public class AmsuFile extends EpsFile {
    
    private static final int PRODUCT_WIDTH = 30;
    private static final String PRODUCT_TYPE = "AMSU-A";
    RadianceBandReader bandReader;

    public AmsuFile(File file, DataFormat format) throws IOException {
        super(file, format);
        bandReader = new RadianceBandReader(0);
    }
    
    @Override
    public Product createProduct() throws IOException {
        Product product = new Product(getProductName(), PRODUCT_TYPE, PRODUCT_WIDTH, getMdrCount());
        addMetaData(product);
        BandInfo[] bandInfos = BandInfo.values();
        for (BandInfo bandInfo : bandInfos) {
            Band band = product.addBand(bandInfo.name, bandInfo.type);
            band.setScalingFactor(bandInfo.scaleFactor);
        }
        return product;
    }
    
    @Override
    public void readBandData(int x, int y, int width, int height, Band band, ProductData buffer, ProgressMonitor pm) throws IOException {
        if (band.getName().equals(BandInfo.RADIANCE01.name)) {
            int i = 0;
            SequenceData mdrData = getMdrData();
            for (int yi = y; yi < y + height; yi++) {
                CompoundData mdr = mdrData.getCompound(yi).getCompound(1);
                i = bandReader.read(x, width, buffer, i, mdr);
            }
        }
    }

    private static class RadianceBandReader {
        private final int index;
        
        public RadianceBandReader(int index) {
            this.index = index;
        }
        
        public int read(int x, int width, ProductData buffer, int i, CompoundData mdr) throws IOException {
            SequenceData radianceSequence = mdr.getSequence("SCENE_RADIANCE");
            for (int xi = x; xi < x + width; xi++) {
                buffer.setElemIntAt(i, radianceSequence.getSequence(xi).getInt(index));
                i++;
            }
            return i;
        }   
    }
}
