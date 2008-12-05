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
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.util.DataPrinter;

import org.jdom.DataConversionException;


public class EpsFile {

    private CompoundData metopData;

    private EpsFile(File file, DataFormat format) throws IOException {
        metopData = format.createContext(file, "r").getData();
    }
    
    public CompoundData getMetopData() {
        return metopData;
    }
    
    public static EpsFile openFile(File file) throws IOException, DataConversionException, URISyntaxException {
        DataFormat mphrFormat = EpsFormats.getInstance().getMPHRformat();
        EpsFile epsFile = new EpsFile(file, mphrFormat);
        CompoundData epsData = epsFile.getMetopData();
        EpsAsciiRecord mphrRecord = new EpsAsciiRecord(epsData.getCompound(1));
        String instrumentId = mphrRecord.getString(mphrRecord.getMemberIndex("INSTRUMENT_ID"));
        String processingLevel = mphrRecord.getString(mphrRecord.getMemberIndex("PROCESSING_LEVEL"));
        int formatMajor = mphrRecord.getInt(mphrRecord.getMemberIndex("FORMAT_MAJOR_VERSION"));
        DataFormat dataFormat = EpsFormats.getInstance().getDataFormat(instrumentId, processingLevel, formatMajor);
        return new EpsFile(file, dataFormat);
    }
    
    public static boolean canOpenFile(File file) throws IOException, DataConversionException, URISyntaxException {
        DataFormat mphrFormat = EpsFormats.getInstance().getMPHRformat();
        EpsFile epsFile = new EpsFile(file, mphrFormat);
        CompoundData epsData = epsFile.getMetopData();
        EpsAsciiRecord mphrRecord = new EpsAsciiRecord(epsData.getCompound(1));
        String instrumentId = mphrRecord.getString(mphrRecord.getMemberIndex("INSTRUMENT_ID"));
        String processingLevel = mphrRecord.getString(mphrRecord.getMemberIndex("PROCESSING_LEVEL"));
        int formatMajor = mphrRecord.getInt(mphrRecord.getMemberIndex("FORMAT_MAJOR_VERSION"));
        return EpsFormats.getInstance().isSupported(instrumentId, processingLevel, formatMajor);
    }

    private static void printValue(EpsAsciiRecord mphrRecord, String memberName) throws IOException {
        int memberIndex = mphrRecord.getMemberIndex(memberName);
        String description = mphrRecord.getDescription(memberIndex);
        System.out.println("description: "+description);
        System.out.println("units: "+mphrRecord.getUnits(memberIndex));
        System.out.println("value: "+mphrRecord.getRawString(memberIndex));
    }
    
    
    public static void main(String[] args) throws IOException, Exception {
        File file = new File(args[0]);
        boolean canOpenFile = EpsFile.canOpenFile(file);
        System.out.println("canOpenFile="+canOpenFile);
        if (canOpenFile) {
            EpsFile epsFile = EpsFile.openFile(file);
        }
//        DataPrinter printer = new DataPrinter();
//        printer.print(epsFile.getMetopData());
    }
}
