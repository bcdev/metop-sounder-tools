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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.core.Assert;

import org.eumetsat.metop.amsu.AmsuFile;
import org.eumetsat.metop.iasi.IasiFile;
import org.eumetsat.metop.mhs.MhsFile;


public class EpsFormats {
    
    private static class FormatDescriptor {

        private final String instrument;
        private final String processingLevel;
        private final int majorVersion;
        private final int minorVersion;

        public FormatDescriptor(String instrument, String processingLevel, int majorVersion, int minorVersion) {
            Assert.notNull(instrument, "instrument");
            Assert.notNull(processingLevel, "processingLevel");
            this.instrument = instrument;
            this.processingLevel = processingLevel;
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
        }
        
        @Override
        public String toString() {
            return instrument + "-" + processingLevel + "_" + majorVersion + "." + minorVersion;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + instrument.hashCode();
            result = prime * result + processingLevel.hashCode();
            result = prime * result + majorVersion;
            result = prime * result + minorVersion;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            FormatDescriptor other = (FormatDescriptor) obj;
            if (!instrument.equals(other.instrument) || 
                    !processingLevel.equals(other.processingLevel) ||
                    majorVersion != other.majorVersion ||
                    minorVersion != other.minorVersion) {
                return false;
            }
            return true;
        }
    }
    
    private interface EpsFileFactory {
        EpsFile create(File file, DataFormat dataFormat) throws IOException;
    }
    
    private class IasiFileFactory implements EpsFileFactory {
        public EpsFile create(File file, DataFormat dataFormat) throws IOException {
            return new IasiFile(file, dataFormat);
        }
    }

    private class MhsFileFactory implements EpsFileFactory {
        public EpsFile create(File file, DataFormat dataFormat) throws IOException {
            return new MhsFile(file, dataFormat);
        }
    }

    private class AmsuFileFactory implements EpsFileFactory {
        public EpsFile create(File file, DataFormat dataFormat) throws IOException {
            return new AmsuFile(file, dataFormat);
        }
    }
    
    private class DefaultFileFactory implements EpsFileFactory {
        public EpsFile create(File file, DataFormat dataFormat) throws IOException {
            return new EpsFile(file, dataFormat);
        }
    }
    
    private final Map<FormatDescriptor, String> formatDescriptors; 
    private final Map<FormatDescriptor, EpsFileFactory> fileFactories; 
    private final Map<String, DataFormat> formats; 
    private DataFormat mphrFormat; 
    private static final EpsFormats INSTANCE = new EpsFormats();
    
    private EpsFormats() {
        FormatDescriptor formatDescriptor;
        formatDescriptors = new HashMap<FormatDescriptor, String>(16);
        fileFactories = new HashMap<FormatDescriptor, EpsFileFactory>(16);
        formats = new HashMap<String, DataFormat>(42);
        
        EpsFileFactory defaultFileFactory = new DefaultFileFactory();
        
        //AVHRR
        formatDescriptor = new FormatDescriptor("AVHR", "1B", 4, 0);
        formatDescriptors.put(formatDescriptor, "eps_avhrrl1b_6.5.xml");
        fileFactories.put(formatDescriptor, defaultFileFactory);
        
        formatDescriptor = new FormatDescriptor("AVHR", "1B", 10, 0);
        formatDescriptors.put(formatDescriptor, "eps_avhrrl1b_6.5.xml");
        fileFactories.put(formatDescriptor, defaultFileFactory);
        
        //IASI
        EpsFileFactory iasiFileFactory = new IasiFileFactory();
        formatDescriptor = new FormatDescriptor("IASI", "1C", 3, 0);
        formatDescriptors.put(formatDescriptor, "eps_iasil1c_6.6.xml");
        fileFactories.put(formatDescriptor, iasiFileFactory);
        
        formatDescriptor = new FormatDescriptor("IASI", "1C", 10, 0);
        formatDescriptors.put(formatDescriptor, "eps_iasil1c_6.6.xml");
        fileFactories.put(formatDescriptor, iasiFileFactory);
        
        //MHS
        EpsFileFactory mhsFileFactory = new MhsFileFactory();
        formatDescriptor = new FormatDescriptor("MHSx", "1B", 3, 0);
        formatDescriptors.put(formatDescriptor, "eps_mhsl1b_6.5.xml");
        fileFactories.put(formatDescriptor, mhsFileFactory);
        
        formatDescriptor = new FormatDescriptor("MHSx", "1B", 10, 0);
        formatDescriptors.put(formatDescriptor, "eps_mhsl1b_6.5.xml");
        fileFactories.put(formatDescriptor, mhsFileFactory);
        
        //AMSU
        EpsFileFactory amsuFileFactory = new AmsuFileFactory();
        formatDescriptor = new FormatDescriptor("AMSA", "1B", 3, 0);
        formatDescriptors.put(formatDescriptor, "eps_amsual1b_6.4.xml");
        fileFactories.put(formatDescriptor, amsuFileFactory);
        
        formatDescriptor = new FormatDescriptor("AMSA", "1B", 10, 0);
        formatDescriptors.put(formatDescriptor, "eps_amsual1b_6.4.xml");
        fileFactories.put(formatDescriptor, amsuFileFactory);
    }
    
    public static EpsFormats getInstance() {
        return INSTANCE;
    }
    
    private DataFormat getDataFormat(FormatDescriptor descriptor) {
        String epsXmlName = formatDescriptors.get(descriptor);
        if (formats.containsKey(epsXmlName)) {
            return formats.get(epsXmlName);
        }
        DataFormat dataFormat = createFormat(epsXmlName);
        formats.put(epsXmlName, dataFormat);
        return dataFormat;
    }
    
    private synchronized DataFormat getMPHRformat() {
        if (mphrFormat == null) {
            mphrFormat = createFormat("mphr.xml");
        }
        return mphrFormat;
    }
    
    private boolean isSupported(FormatDescriptor descriptor) {
        return formatDescriptors.containsKey(descriptor);
    }
    
    public EpsFile openFile(File file) throws IOException {
        FormatDescriptor formatDescriptor = readFormatDescriptor(file);
        DataFormat dataFormat = getDataFormat(formatDescriptor);
        EpsFileFactory epsFileFactory = fileFactories.get(formatDescriptor);
        if (epsFileFactory != null) {
            return epsFileFactory.create(file, dataFormat);
        }
        return null;
    }
    
    public boolean canOpenFile(File file) throws IOException {
        FormatDescriptor formatDescriptor = readFormatDescriptor(file);
        return isSupported(formatDescriptor);
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
    
    private DataFormat createFormat(String epsXmlName) {
        URL resource = EpsFormats.class.getResource(epsXmlName);
        EpsXml epsXml;
        try {
            epsXml = new EpsXml(resource.toURI());
        } catch (Exception e) {
            throw new IllegalArgumentException("Problems creating format for " + epsXmlName, e);
        }
        return epsXml.getFormat();
    }
}
