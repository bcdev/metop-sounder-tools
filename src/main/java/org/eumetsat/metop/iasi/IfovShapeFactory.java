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
package org.eumetsat.metop.iasi;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.math.MathUtils;

import java.io.IOException;


class IfovShapeFactory {
    private static final double Re = 6378137;
    private static final double H = 819000;
    
    private final IasiFile iasiFile;
    private final Product avhrrProduct;
    private final GeoCoding geoCoding;

    private final GeoPos nadirGP;
    private double[] shapeScales;
    
    public IfovShapeFactory(IasiFile iasiFile, Product avhrrProduct) throws IOException {
        this.iasiFile = iasiFile;
        this.avhrrProduct = avhrrProduct;
        this.geoCoding = avhrrProduct.getGeoCoding();
        
        boolean goodLine = false;
        int mdrIndex = 0;
        while(!goodLine) {
            boolean ivof1InProduct = isIfovInProduct(IasiFile.computeIfovId(0, 14, 0)); 
            boolean ivof2InProduct = isIfovInProduct(IasiFile.computeIfovId(0, 15, 0));
            if (ivof1InProduct && ivof2InProduct) {
                goodLine = true;
            } else {
                mdrIndex++;
            }
        }
        GeoPos[] ifovGeoPos = new GeoPos[30*2];
        int i = 0;
        for (int efovIndex = 0; efovIndex < 30; efovIndex++) {
            ifovGeoPos[i] = iasiFile.readGeoPos(IasiFile.computeIfovId(mdrIndex, efovIndex, 0));
            i++;
            ifovGeoPos[i] = iasiFile.readGeoPos(IasiFile.computeIfovId(mdrIndex, efovIndex, 3));
            i++;
        }
        this.nadirGP = computeNadirGeoPos(ifovGeoPos);
        double[][] vza = iasiFile.readVZA(mdrIndex);
        double[] firstLineVza = new double[30 * 2];
        i = 0;
        for (int efovIndex = 0; efovIndex < vza.length; efovIndex++) {
            firstLineVza[i] = vza[efovIndex][0];
            i++;
            firstLineVza[i] = vza[efovIndex][3];
            i++;
        }
        shapeScales = computeShapeScales(ifovGeoPos, firstLineVza);
    }
    
    public double[] getIfovShapeScale() {
        return shapeScales;
    }
    
    private double[] computeShapeScales(GeoPos[] ifovGeoPos, double[] vza) {
        double[] scales = new double[30 * 2];
        for (int i = 0; i < scales.length; i++) {
            GeoPos gp = ifovGeoPos[i];
            double distance = MathUtils.sphereDistanceDeg(Re, nadirGP.lon, nadirGP.lat, gp.lon, gp.lat);
            double theta = MathUtils.DTOR * vza[i];
            double alpha = distance / Re;
            double hTheta = Math.sin(alpha) * (H + Re) / Math.sin(theta);
            scales[i] = hTheta/H;
        }
        return scales;
    }

    private GeoPos computeNadirGeoPos(GeoPos[] ifovGeoPos) {
        final int length = ifovGeoPos.length;
        GeoPos gp1 = ifovGeoPos[(length/2)];
        GeoPos gp2 = ifovGeoPos[(length/2)-1];
        PixelPos pp1 = geoCoding.getPixelPos(gp1, null);
        PixelPos pp2 = geoCoding.getPixelPos(gp2, null);
        PixelPos nadir = new PixelPos((pp1.x+pp2.x)/2, (pp1.y+pp2.y)/2);
        return geoCoding.getGeoPos(nadir, null);
    }

    private boolean isIfovInProduct(int ifovId) throws IOException {
        GeoPos gp1 = iasiFile.readGeoPos(ifovId);
        PixelPos pp1 = geoCoding.getPixelPos(gp1, null);
        return avhrrProduct.containsPixel(pp1);
    }
}
