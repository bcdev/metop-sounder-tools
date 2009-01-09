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
package org.eumetsat.metop.visat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.util.math.MathUtils;

import java.awt.Color;
import java.awt.image.Raster;

public class SounderOverlayModel {

    private Band band;
    private Raster rawData;
    private Color[] colorPalette;
    private ColorPaletteDef paletteDef;

    public SounderOverlayModel(Band band) {
        setBand(band);
    }
    
    public void setBand(Band band) {
        this.band = band;
        rawData = band.getSourceImage().getData();
        computeColorPalette();
    }
    
    public Band getBand() {
        return band;
    }
    
    public Color getColor(int x, int y) {
        double sample = rawData.getSampleDouble(x, y, 0);
        int numColors = colorPalette.length;
        double min = paletteDef.getMinDisplaySample();
        double max = paletteDef.getMaxDisplaySample();
        int index = MathUtils.floorAndCrop((sample - min) * (numColors - 1.0) / (max - min), 0, numColors-1);
        return colorPalette[index];
    }
    
    private void computeColorPalette() {
        final int width = band.getSceneRasterWidth();
        final int height = band.getSceneRasterHeight();
        int valueMin = Integer.MAX_VALUE;
        int valueMax = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int value = rawData.getSample(x, y, 0);
                valueMin = Math.min(valueMin, value);
                valueMax = Math.max(valueMax, value);
            }
        }
        paletteDef = new ColorPaletteDef(valueMin, valueMax);
        colorPalette = paletteDef.createColorPalette(Scaling.IDENTITY);
    }
}
