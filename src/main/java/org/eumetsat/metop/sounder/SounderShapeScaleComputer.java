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

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.math.MathUtils;
import org.eumetsat.metop.eps.EpsFile;

import java.io.IOException;

import com.bc.ceres.core.ProgressMonitor;


public class SounderShapeScaleComputer {
    private static final double Re = 6378137;
    private static final double H = 819000;

    private final int width;
    private double[] shapeScales;
    
    public SounderShapeScaleComputer(EpsFile epsFile, int width, BandInfo lat, BandInfo lon, BandInfo vza) throws IOException {
        this.width = width;

        double[] latData = readScaledLine(epsFile, lat, 0);
        double[] lonData = readScaledLine(epsFile, lon, 0);
        double[] vzaData = readScaledLine(epsFile, vza, 0);
        
        shapeScales = computeShapeScales(latData, lonData, vzaData);
    }
    
    public double[] getIfovShapeScale() {
        return shapeScales;
    }
    
    private double[] readScaledLine(EpsFile epsFile, BandInfo bandInfo, int mdrIndex) throws IOException {
        ProductData productData = ProductData.createInstance(bandInfo.getType(), width);
        epsFile.readData(bandInfo.getReader(), 0, mdrIndex, width, 1, productData , ProgressMonitor.NULL);
        final int length = productData.getNumElems();
        double[] scaled = new double[length];
        for (int i = 0; i < scaled.length; i++) {
            scaled[i] = productData.getElemIntAt(i) * bandInfo.getScaleFactor();
        }
        return scaled;
    }
    
    private double[] computeShapeScales(double[] lat, double[] lon, double[] vza) {
        double[] scales = new double[lat.length];
        for (int i = 0; i < scales.length; i++) {
            final int j = scales.length - 1 - i;
            double distance = MathUtils.sphereDistanceDeg(Re, lon[j], lat[j], lon[i], lat[i]) / 2.0;
            double theta = MathUtils.DTOR * (vza[i] + vza[j]) / 2.0;
            double alpha = distance / Re;
            double hTheta = Math.sin(alpha) * (H + Re) / Math.sin(theta);
            scales[i] = hTheta/H;
        }
        return scales;
    }
}
