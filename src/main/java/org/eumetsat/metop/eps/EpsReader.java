/*
 * AVISA software - $Id: EpsReader.java,v 1.1.1.1 2007/03/22 11:12:51 ralf Exp $
 *
 * Copyright (C) 2005 by EUMETSAT
 *
 * The Licensee acknowledges that the AVISA software is owned by the European
 * Organisation for the Exploitation of Meteorological Satellites
 * (EUMETSAT) and the Licensee shall not transfer, assign, sub-licence,
 * reproduce or copy the AVISA software to any third party or part with
 * possession of this software or any part thereof in any way whatsoever.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * The AVISA software has been developed using the ESA BEAM software which is
 * distributed under the GNU General Public License (GPL).
 *
 */
package org.eumetsat.metop.eps;

import com.bc.ceres.core.ProgressMonitor;

import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * A reader for METOP-AVHRR/3 Level-1b data products.
 */
public class EpsReader extends AbstractProductReader {

    private EpsFile epsFile;

    public EpsReader(ProductReaderPlugIn metopReaderPlugIn) {
        super(metopReaderPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code>
     * interface method. Clients implementing this method can be sure that the
     * input object and eventually the subset information has already been set.
     * <p/>
     * <p/>
     * This method is called as a last step in the
     * <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File file = EpsReaderPlugIn.getInputFile(getInput());

        Product product;
        try {
            epsFile = EpsFormats.getInstance().openFile(file);
            product = epsFile.createProduct(this);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                close();
            } catch (IOException ignored) {
                // ignore
            }
            throw e;
        }
        product.setFileLocation(file);
        return product;
    }
    
    @Override
    public void close() throws IOException {
        epsFile.close();
        super.close();
    }
    
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        epsFile.readBandData(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, destBand, destBuffer, pm);
    }
    
    public EpsFile getEpsFile() {
        return epsFile;
    }

    public static boolean canOpenFile(File file) {
        try {
            return EpsFormats.getInstance().canOpenFile(file);
        } catch (IOException e) {
            return false;
        }
    }

}