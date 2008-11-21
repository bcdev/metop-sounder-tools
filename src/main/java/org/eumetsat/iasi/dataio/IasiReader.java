/*
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.iasi.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.IOException;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision: 80 $ $Date: 2008-01-10 11:26:23 +0100 (Do, 10 Jan 2008) $
 */
public class IasiReader extends AbstractProductReader {

    private IasiFile iasiFile;

    protected IasiReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {

        try {
            final File file = (File) getInput();
            iasiFile = new IasiFile(file, 0, 0, 0, 0);
            iasiFile.readHeader();
//            createProduct();
//            product.setFileLocation(file);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                close();
            } catch (IOException ignored) {
            }
            throw e;
        }

        return null;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          Band destBand,
                                          int destOffsetX,
                                          int destOffsetY,
                                          int destWidth,
                                          int destHeight,
                                          ProductData destBuffer, ProgressMonitor pm) throws IOException {
        // todo - implement
    }

    @Override
    public void close() throws IOException {
        iasiFile.close();
        iasiFile = null;
        super.close();
    }


}
