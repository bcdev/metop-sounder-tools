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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.bc.ceres.binio.DataFormat;



public class EpsFormats {
    
    static class FormatDescriptor {

        private final String instrument;
        private final String processingLevel;
        private final int majorVersion;
        private final int minorVersion;

        public FormatDescriptor(String instrument, String processingLevel, int majorVersion, int minorVersion) {
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
            result = prime * result + ((instrument == null) ? 0 : instrument.hashCode());
            result = prime * result + ((processingLevel == null) ? 0 : processingLevel.hashCode());
            result = prime * result + majorVersion;
            result = prime * result + minorVersion;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FormatDescriptor other = (FormatDescriptor) obj;
            if (instrument == null) {
                if (other.instrument != null)
                    return false;
            } else if (!instrument.equals(other.instrument))
                return false;
            if (majorVersion != other.majorVersion)
                return false;
            if (minorVersion != other.minorVersion)
                return false;
            if (processingLevel == null) {
                if (other.processingLevel != null)
                    return false;
            } else if (!processingLevel.equals(other.processingLevel))
                return false;
            return true;
        }
    }
    
    private final Map<FormatDescriptor, String> formatDescriptors; 
    private final Map<String, DataFormat> formats; 
    private DataFormat mphrFormat; 
    private static final EpsFormats INSTANCE = new EpsFormats();
    
    private EpsFormats() {
        formatDescriptors = new HashMap<FormatDescriptor, String>(12);
        formatDescriptors.put(new FormatDescriptor("AVHR", "1B", 4, 0), "eps_avhrrl1b_6.5.xml");
        formatDescriptors.put(new FormatDescriptor("AVHR", "1B", 10, 0), "eps_avhrrl1b_6.5.xml");
        formatDescriptors.put(new FormatDescriptor("IASI", "1C", 10, 0), "eps_iasil1c_6.6.xml");
        formats = new HashMap<String, DataFormat>(42);
    }
    public static EpsFormats getInstance() {
        return INSTANCE;
    }
    
    public DataFormat getDataFormat(FormatDescriptor descriptor) {
        String epsXmlName = formatDescriptors.get(descriptor);
        if (formats.containsKey(epsXmlName)) {
            return formats.get(epsXmlName);
        }
        DataFormat dataFormat = createFormat(epsXmlName);
        formats.put(epsXmlName, dataFormat);
        return dataFormat;
    }
    
    public synchronized DataFormat getMPHRformat() {
        if (mphrFormat == null) {
            mphrFormat = createFormat("mphr.xml");
        }
        return mphrFormat;
    }
    
    public boolean isSupported(FormatDescriptor descriptor) {
        return formatDescriptors.containsKey(descriptor);
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
}
