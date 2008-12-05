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

import com.bc.ceres.binio.DataFormat;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class EpsFormats {
    
    private static class FormatDescriptor {

        private final String instrument;
        private final String processingLevel;
        private final int formatVersion;
        private final String epsXmlName;

        public FormatDescriptor(String instrument, String processingLevel, int formatVersion, String epsXmlName) {
            this.instrument = instrument;
            this.processingLevel = processingLevel;
            this.formatVersion = formatVersion;
            this.epsXmlName = epsXmlName;
        }
        
    }
    
    private final List<FormatDescriptor> formatDescriptors; 
    private final Map<String, DataFormat> formats; 
    private DataFormat mphrFormat; 
    private static final EpsFormats INSTANCE = new EpsFormats();
    
    private EpsFormats() {
        formatDescriptors = new ArrayList<FormatDescriptor>(12);
        formatDescriptors.add(new FormatDescriptor("AVHR", "1B", 4, "eps_avhrrl1b_6.5.xml"));
        formatDescriptors.add(new FormatDescriptor("AVHR", "1B", 10, "eps_avhrrl1b_6.5.xml"));
        formatDescriptors.add(new FormatDescriptor("IASI", "1C", 3, "eps_iasil1c_6.6.xml"));
        formats = new HashMap<String, DataFormat>(42);
    }
    public static EpsFormats getInstance() {
        return INSTANCE;
    }
    
    public DataFormat getDataFormat(String instrument, String processingLevel, int formatVersion) {
        FormatDescriptor fd = getFormatDescriptor(instrument, processingLevel, formatVersion);
        if (formats.containsKey(fd.epsXmlName)) {
            return formats.get(fd.epsXmlName);
        }
        DataFormat dataFormat = createFormat(fd.epsXmlName);
        formats.put(fd.epsXmlName, dataFormat);
        return dataFormat;
    }
    
    public synchronized DataFormat getMPHRformat() {
        if (mphrFormat == null) {
            mphrFormat = createFormat("mphr.xml");
        }
        return mphrFormat;
    }
    
    public boolean isSupported(String instrument, String processingLevel, int formatVersion) {
        FormatDescriptor fd = getFormatDescriptor(instrument, processingLevel, formatVersion);
        return fd != null; 
    }
    
    private DataFormat createFormat(String epsXmlName) {
        URL resource = this.getClass().getResource(epsXmlName);
        EpsXml epsXml;
        try {
            epsXml = new EpsXml(resource.toURI());
        } catch (Exception e) {
            throw new IllegalArgumentException("Problems creating format for " + epsXmlName);
        }
        return epsXml.getFormat();
    }
    
    private FormatDescriptor getFormatDescriptor(String instrument, String processingLevel, int formatVersion) {
        for (FormatDescriptor fd : formatDescriptors) {
            if (fd.instrument.equals(instrument) &&
                fd.processingLevel.equals(processingLevel) &&
                fd.formatVersion == formatVersion ) {
            return fd;
            }
        }
        return null;
    }
}
