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

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.util.DataPrinter;

import org.eumetsat.metop.binio.EpsFormats.FormatDescriptor;


public class EpsFile {

    private CompoundData metopData;

    private EpsFile(File file, DataFormat format) throws IOException {
        metopData = format.createContext(file, "r").getData();
    }
    
    public CompoundData getMetopData() {
        return metopData;
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
        File file = new File(args[0]);
        File[] listFiles = file.listFiles();
        for (File epsFile : listFiles) {
            System.out.print("file="+epsFile.getName()+", ");
            boolean canOpenFile = EpsFile.canOpenFile(epsFile);
            System.out.print("canOpen="+canOpenFile+", ");
            FormatDescriptor formatDescriptor = readFormatDescriptor(epsFile);
            System.out.println(formatDescriptor);
            if (canOpenFile) {
//                EpsFile epsFile = EpsFile.openFile(file);
            }
        }
//        DataPrinter printer = new DataPrinter();
//        printer.print(epsFile.getMetopData());
    }
}
