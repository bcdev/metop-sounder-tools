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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.util.DataPrinter;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.eumetsat.metop.binio.EpsFormats.FormatDescriptor;


public class EpsFile {

    private CompoundData metopData;

    private EpsFile(File file, DataFormat format) throws IOException {
        metopData = format.createContext(file, "r").getData();
    }
    
    public CompoundData getMetopData() {
        return metopData;
    }
    
    public CompoundData getMphrData() throws IOException {
        return metopData.getCompound(0).getCompound(0).getCompound(1);
    }
    
    public List<MetadataElement> getMetaData() throws IOException {
        List<MetadataElement> metaDataList = new ArrayList<MetadataElement>(20);
        
        CompoundData header = metopData.getCompound(0);
        final int headerCount = header.getMemberCount();
        for (int i = 0; i < headerCount; i++) {
            EpsAsciiRecord asciiRecord = new EpsAsciiRecord(header.getCompound(i).getCompound(1));
            metaDataList.add(asciiRecord.getAsMetaDataElement());
        }
        CompoundData body = metopData.getSequence(2).getCompound(0);
        int bodyCount = body.getMemberCount();
        DataPrinter printer = new DataPrinter();
        for (int i = 0; i < bodyCount; i++) {
            SequenceData sequence = body.getSequence(i);
            if (sequence.getCompound(0).getCompoundType().getName().equals("dummy")) {
                // skip unkown types
                continue;
            }
            int elementCount = sequence.getElementCount();
            if (elementCount == 1 ) {
                CompoundData compound = sequence.getCompound(0).getCompound(1);
                EpsAsciiRecord binRecord = new EpsAsciiRecord(compound, false);
                metaDataList.add(binRecord.getAsMetaDataElement());
            } else {
//                MetadataElement metadataElement = new MetadataElement();
                System.out.println("FOOOOOOOOOOOOOOOOO");
            }
//            CompoundData binRecord = sequence.getCompound(1);
//            System.out.println(binRecord.getCompoundType().getName());
        }
        
//        printer.print(body);
        
        return metaDataList;
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
        EpsAsciiRecord mphrRecord = new EpsAsciiRecord(epsData.getCompound(1));
        String instrumentId = mphrRecord.getString(mphrRecord.getMemberIndex("INSTRUMENT_ID"));
        String processingLevel = mphrRecord.getString(mphrRecord.getMemberIndex("PROCESSING_LEVEL"));
        int majorVersion = mphrRecord.getInt(mphrRecord.getMemberIndex("FORMAT_MAJOR_VERSION"));
        int minorVersion = mphrRecord.getInt(mphrRecord.getMemberIndex("FORMAT_MINOR_VERSION"));
        return new EpsFormats.FormatDescriptor(instrumentId, processingLevel, majorVersion, minorVersion);
    }

    
    public static void main(String[] args) throws IOException, Exception {
        File dir = new File(args[0]);
        File[] listFiles = dir.listFiles();
        for (File file : listFiles) {
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
//                EpsAsciiRecord mphr = new EpsAsciiRecord(epsFile.getMphrData());
            }
            System.exit(1);
        }
    }
}
