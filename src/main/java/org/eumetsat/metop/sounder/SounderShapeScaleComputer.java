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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.math.MathUtils;
import org.eumetsat.metop.eps.EpsFile;

import java.io.IOException;

import com.bc.ceres.core.ProgressMonitor;


public class SounderShapeScaleComputer {
    private static final double Re = 6378137;
    private static final double H = 819000;
    
    private final EpsFile epsFile;
    private final int width;
    private final BandInfo latBandInfo;
    private final BandInfo lonBandInfo;
    private final BandInfo vzaBandInfo;
    private final Product avhrrProduct;
    private final GeoCoding geoCoding;

    private final GeoPos nadirGP;
    private double[] shapeScales;
    
    public SounderShapeScaleComputer(EpsFile epsFile, int width, BandInfo lat, BandInfo lon, BandInfo vza, Product avhrrProduct) throws IOException {
        this.epsFile = epsFile;
        this.width = width;
        this.latBandInfo = lat;
        this.lonBandInfo = lon;
        this.vzaBandInfo = vza;
        this.avhrrProduct = avhrrProduct;
        this.geoCoding = avhrrProduct.getGeoCoding();
        
        boolean goodLine = false;
        int mdrIndex = 0;
        double[] latData;
        double[] lonData;
        do {
            latData = readScaledLine(latBandInfo, mdrIndex);
            lonData = readScaledLine(lonBandInfo, mdrIndex);
            
            boolean ivof1InProduct = isIfovInProduct(latData[(width/2)], lonData[(width/2)]); 
            boolean ivof2InProduct = isIfovInProduct(latData[(width/2)-1], lonData[(width/2)-1]); 
            if (ivof1InProduct && ivof2InProduct) {
                goodLine = true;
            } else {
                mdrIndex++;
            }
        } while (!goodLine);
        double[] vzaData = readScaledLine(vzaBandInfo, mdrIndex);
        
        this.nadirGP = computeNadirGeoPos(latData, lonData);
        shapeScales = computeShapeScales(latData, lonData, vzaData);
    }
    
    public double[] getIfovShapeScale() {
        return shapeScales;
    }
    
    private double[] readScaledLine(BandInfo bandInfo, int mdrIndex) throws IOException {
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
            double distance = MathUtils.sphereDistanceDeg(Re, nadirGP.lon, nadirGP.lat, lon[i], lat[i]);
            double theta = MathUtils.DTOR * vza[i];
            double alpha = distance / Re;
            double hTheta = Math.sin(alpha) * (H + Re) / Math.sin(theta);
            scales[i] = hTheta/H;
        }
        return scales;
    }

    private GeoPos computeNadirGeoPos(double[] lat, double[] lon) {
        final int length = lat.length;
        GeoPos gp1 = new GeoPos((float)lat[(length/2)], (float)lon[(length/2)]);
        GeoPos gp2 = new GeoPos((float)lat[(length/2)-1], (float)lon[(length/2)-1]);
        PixelPos pp1 = geoCoding.getPixelPos(gp1, null);
        PixelPos pp2 = geoCoding.getPixelPos(gp2, null);
        PixelPos nadir = new PixelPos((pp1.x+pp2.x)/2, (pp1.y+pp2.y)/2);
        return geoCoding.getGeoPos(nadir, null);
    }

    private boolean isIfovInProduct(double lat, double lon) {
        GeoPos gp1 = new GeoPos((float)lat, (float)lon);
        PixelPos pp1 = geoCoding.getPixelPos(gp1, null);
        return avhrrProduct.containsPixel(pp1);
    }
}
