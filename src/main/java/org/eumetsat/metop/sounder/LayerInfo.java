/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;

class LayerInfo {
    
    private final Scaling scaling;
    private final Stx stx;
    private final ImageInfo imageInfo;

    private LayerInfo(Scaling scaling, Stx stx, ImageInfo imageInfo) {
        this.scaling = scaling;
        this.stx = stx;
        this.imageInfo = imageInfo;
    }

    static LayerInfo createInstance(Band band) {
        final double scalingFactor = band.getScalingFactor();

        final Scaling scaling = new Scaling() {
            @Override
            public double scale(double value) {
                return scalingFactor * value;
            }

            @Override
            public double scaleInverse(double value) {
                return value / scalingFactor;
            }
        };
        final Stx stx = Stx.create(band, 0, ProgressMonitor.NULL);

        final double min = scaling.scale(stx.getMin());
        final double max = scaling.scale(stx.getMax());
        final double center = scaling.scale(stx.getMean());

        final ImageInfo imageInfo = new ImageInfo(new ColorPaletteDef(min, center, max));

        return new LayerInfo(scaling, stx, imageInfo);
    }

    public Scaling getScaling() {
        return scaling;
    }

    public Stx getStx() {
        return stx;
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }
}
