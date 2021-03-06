/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.eumetsat.metop.sounder;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.SequenceData;

import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;


public class LocationReader implements MdrReader {

    private final int index;
    
    public LocationReader(int index) {
        this.index = index;
    }
    
    @Override
    public int read(int x, int width, ProductData buffer, int bufferIndex, CompoundData mdr) throws IOException {
        SequenceData locationSequence = mdr.getSequence("EARTH_LOCATION");
        for (int xi = x; xi < x + width; xi++) {
            buffer.setElemIntAt(bufferIndex, locationSequence.getSequence(xi).getInt(index));
            bufferIndex++;
        }
        return bufferIndex;
    }

}
