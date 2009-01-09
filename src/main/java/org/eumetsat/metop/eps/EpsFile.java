/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package org.eumetsat.metop.eps;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.core.ProgressMonitor;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.eumetsat.metop.visat.AvhrrOverlay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class EpsFile {

    private final CompoundData metopData;
    private final DataContext dataContext;
    private int mdrCount = -1;
    private EpsRecord mphr;
    private Product product;

    protected EpsFile(File file, DataFormat format) throws IOException {
        dataContext = format.createContext(file, "r");
        metopData = dataContext.getData();
    }
    
    public synchronized Product createProduct(ProductReader reader) throws IOException {
        if (product == null ) {
            product = createProductImpl(reader);
        }
        return product;
    }
    
    public Product getProduct() {
        return product;
    }
    
    protected Product createProductImpl(ProductReader reader) throws IOException {
        Product newProduct = new Product("Dummy", "EPS", 1, 1, reader);
        MetadataElement metadataRoot = newProduct.getMetadataRoot();
        List<MetadataElement> metaData = getMetaData();
        for (MetadataElement metadataElement : metaData) {
            metadataRoot.addElement(metadataElement);
        }
        return newProduct;
    }
    
    public boolean hasOverlayFor(Product avhrrProduct) {
        return false;
    }
    
    public AvhrrOverlay createOverlay(Product avhrrProduct) {
        return null;
    }
    
    public void close() {
        dataContext.dispose();
    }
    
    public synchronized String getProductName() throws IOException {
        if (mphr == null) {
            mphr = new EpsRecord(getMphrData(), true);
        }
        return mphr.getString(0);
    }
    
    public synchronized int getMdrCount() throws IOException {
        if (mdrCount == -1) {
            mdrCount = getMdrData().getElementCount();
        }
        return mdrCount;
    }
    
    public void readBandData(int x, int y, int width, int height, Band band, ProductData buffer, ProgressMonitor pm) throws IOException {
    }

    
    protected void addMetaData(Product product) throws IOException {
        MetadataElement metadataRoot = product.getMetadataRoot();
        List<MetadataElement> metaData = getMetaData();
        for (MetadataElement metadataElement : metaData) {
            metadataRoot.addElement(metadataElement);
        }
    }
    
    public CompoundData getMetopData() {
        return metopData;
    }
 
    public CompoundData getMphrData() throws IOException {
        return metopData.getCompound(0).getCompound(0).getCompound(1);
    }
    
    public CompoundData getAuxDataRecord(String name) throws IOException {
        CompoundData body = metopData.getSequence(2).getCompound(0);
        int numBodyElems = body.getMemberCount();
        for (int i = 0; i < numBodyElems; i++) {
            CompoundData compound = body.getSequence(i).getCompound(0);
            String recordType = compound.getCompoundType().getName();
            if (recordType.equals(name)) {
                return compound;
            }
        }
        return null;
    }
    
    public SequenceData getMdrData() throws IOException {
        CompoundData body = metopData.getSequence(2).getCompound(0);
        int numBodyElems = body.getMemberCount();
        for (int i = 0; i < numBodyElems; i++) {
            SequenceData sequence = body.getSequence(i);
            String recordType = sequence.getSequenceType().getName();
            if (recordType.startsWith("mdr")) {
                return sequence;
            }
        }
        return null;
    }
    
    public List<MetadataElement> getMetaData() throws IOException {
        List<MetadataElement> metaDataList = new ArrayList<MetadataElement>(20);
        
        CompoundData header = metopData.getCompound(0);
        final int headerCount = header.getMemberCount();
        for (int i = 0; i < headerCount; i++) {
            EpsRecord epsRecord = new EpsRecord(header.getCompound(i).getCompound(1), true);
            metaDataList.add(epsRecord.getAsMetaDataElement());
        }
        CompoundData body = metopData.getSequence(2).getCompound(0);
        int numBodyElems = body.getMemberCount();
        for (int i = 0; i < numBodyElems; i++) {
            SequenceData sequence = body.getSequence(i);
            String recordType = sequence.getCompound(0).getCompoundType().getName();
            if (recordType.equals("dummy")) {
                // skip unkown types
                continue;
            }
            if (recordType.startsWith("mdr")) {
                // measurement data is not metadata
                break;
            }
            int elementCount = sequence.getElementCount();
            if (elementCount == 1 ) {
                CompoundData compound = sequence.getCompound(0).getCompound(1);
                EpsRecord binRecord = new EpsRecord(compound, false);
                metaDataList.add(binRecord.getAsMetaDataElement());
            } else {
                System.out.println("elemcount "+elementCount);
                System.out.println("Not implemented yet");
//                MetadataElement metadataElement = new MetadataElement();
            }
        }
        return metaDataList;
    }
    
    public static double readVInt4(CompoundData data) throws IOException {
        final byte scaleFactor = data.getByte(0);
        final int value = data.getInt(1);

        return value / Math.pow(10.0, scaleFactor);
    }
    
    public static ProductData.UTC readShortCdsTime(CompoundData data) throws IOException {
        final int day = data.getUShort(0);
        final long millis = data.getUInt(1);

        final long seconds = millis / 1000;
        final long micros = (millis - seconds * 1000) * 1000;
        
        return new ProductData.UTC(day, (int) seconds, (int) micros);
    }
    
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        if (dir.isDirectory()) {
            File[] listFiles = dir.listFiles();
            for (File file : listFiles) {
                handleFile(file);
            }
        } else {
            handleFile(dir);
        }
    }
    
    public static void handleFile(File file) throws Exception {
            System.out.print("file="+file.getName()+", ");
            boolean canOpenFile = EpsFormats.getInstance().canOpenFile(file);
            System.out.println("canOpen="+canOpenFile);
            if (canOpenFile) {
                EpsFile epsFile = EpsFormats.getInstance().openFile(file);
                int elementCount = epsFile.getMdrData().getElementCount();
                System.out.println("num MDRs "+elementCount);

//                DataPrinter printer = new DataPrinter();
//                printer.print(epsFile.getMetopData());
                List<MetadataElement> metaData = epsFile.getMetaData();
//                for (MetadataElement metadataElement : metaData) {
//                    System.out.println(metadataElement.getName());
//                    for (MetadataAttribute attribute : metadataElement.getAttributes()) {
//                        System.out.println( "  "+attribute.getName()+ " : "+attribute.getData().toString()+ " ["
//                                            + attribute.getUnit()+ "] "+ attribute.getDescription());
//                    }
//                }
                epsFile.close();
            }
        }


}
