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
package org.eumetsat.metop.binio;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.eumetsat.metop.binio.EpsFormats.FormatDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.stream.ImageInputStream;


public class EpsFile {

    private final CompoundData metopData;
    private final DataContext dataContext;

    private EpsFile(File file, DataFormat format) throws IOException {
        dataContext = format.createContext(file, "r");
        metopData = dataContext.getData();
    }
    
    public void close() {
        dataContext.dispose();
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
    
    public static EpsFile openFile(File file) throws IOException {
        FormatDescriptor formatDescriptor = readFormatDescriptor(file);
        DataFormat dataFormat = EpsFormats.getInstance().getDataFormat(formatDescriptor);
        return new EpsFile(file, dataFormat);
    }
    
    public static boolean canOpenFile(File file) throws IOException {
        FormatDescriptor formatDescriptor = readFormatDescriptor(file);
        return EpsFormats.getInstance().isSupported(formatDescriptor);
    }
    
    private static FormatDescriptor readFormatDescriptor(File file) throws IOException {
        DataFormat mphrFormat = EpsFormats.getInstance().getMPHRformat();
        EpsFile epsFile = new EpsFile(file, mphrFormat);
        CompoundData epsData = epsFile.getMetopData();
        EpsRecord mphrRecord = new EpsRecord(epsData.getCompound(1), true);
        String instrumentId = mphrRecord.getString(mphrRecord.getMemberIndex("INSTRUMENT_ID"));
        String processingLevel = mphrRecord.getString(mphrRecord.getMemberIndex("PROCESSING_LEVEL"));
        int majorVersion = mphrRecord.getInt(mphrRecord.getMemberIndex("FORMAT_MAJOR_VERSION"));
        int minorVersion = mphrRecord.getInt(mphrRecord.getMemberIndex("FORMAT_MINOR_VERSION"));
        return new EpsFormats.FormatDescriptor(instrumentId, processingLevel, majorVersion, minorVersion);
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
            boolean canOpenFile = EpsFile.canOpenFile(file);
            System.out.print("canOpen="+canOpenFile+", ");
            FormatDescriptor formatDescriptor = readFormatDescriptor(file);
            System.out.println(formatDescriptor);
            if (canOpenFile) {
                EpsFile epsFile = EpsFile.openFile(file);

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
