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
package org.eumetsat.metop.amsu;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.SequenceData;

import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;


class FlagReader implements MdrReader {

    private final String memberName;
    
    FlagReader(String memberName) {
        this.memberName = memberName;
    }

    @Override
    public int read(int x, int width, ProductData buffer, int bufferIndex, CompoundData mdr) throws IOException {
        SequenceData dataSequence = mdr.getSequence(memberName);
        for (int xi = x; xi < x + width; xi++) {
            short value = dataSequence.getShort(xi);
            System.out.println(xi+" "+value);
            buffer.setElemIntAt(bufferIndex, value);
            bufferIndex++;
        }
        return bufferIndex;
    }
}