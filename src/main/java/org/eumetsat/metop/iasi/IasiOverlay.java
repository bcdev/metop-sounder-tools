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

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.eumetsat.metop.sounder.Ifov;
import org.eumetsat.metop.sounder.SounderOverlay;
import org.eumetsat.metop.sounder.SounderOverlayListener;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.bc.ceres.binio.CompoundData;


public class IasiOverlay implements SounderOverlay {

    private static final int PN = 4;
    private static final int SNOT = 30;

    private static final int IFOV_SIZE = 12;
    private static final float IFOV_DIST = 18;

    private final IasiFile iasiFile;
    private final Product avhrrProduct;
    private final long avhrrStartMillis;
    private final long avhrrEndMillis;
    private final int avhrrRasterHeight;
    private final int avhrrTrimLeft;

    private final int mdrCount;
    private Efov[] overlayEfovs;
    private final Map<SounderOverlayListener, Object> listenerMap;
    private Ifov selectedIfov;
    private double iDefSpectDWn1b;
    private double iDefNFirst1b;
    private final EfovShapeFactory efovShapeFactory;
    private final IfovShapeFactory ifovShapeFactory;
    private final IfovPosProvider ifovPosProvider;

    public IasiOverlay(IasiFile iasiFile, Product avhrrProduct) throws IOException {
        this.iasiFile = iasiFile;
        this.avhrrProduct = avhrrProduct;
        avhrrEndMillis = avhrrProduct.getEndTime().getAsCalendar().getTimeInMillis();
        avhrrStartMillis = avhrrProduct.getStartTime().getAsCalendar().getTimeInMillis();
        avhrrTrimLeft = avhrrProduct.getMetadataRoot().getElement("READER_INFO").getAttributeInt("TRIM_LEFT", 0);
        avhrrRasterHeight = avhrrProduct.getSceneRasterHeight();
        listenerMap = Collections.synchronizedMap(new WeakHashMap<SounderOverlayListener, Object>());
        mdrCount = iasiFile.getMdrCount();
        iDefSpectDWn1b = iasiFile.readIDefSpectDWn1b(0);
        iDefNFirst1b = iasiFile.readIDefNsfirst1b(0);
        efovShapeFactory = new FastEfovShape();
        ifovShapeFactory = new EfovDistributionShapeFactory();
        ifovPosProvider = new AvhrrPixelPosBasedProvider();
    }

    @Override
    public Product getAvhrrProduct() {
        return avhrrProduct;
    }

    @Override
    public IasiFile getEpsFile() {
        return iasiFile;
    }

    @Override
    public Ifov getSelectedIfov() {
        return selectedIfov;
    }

    @Override
    public void setSelectedIfov(Ifov ifov) {
        if (ifov != selectedIfov) {
            selectedIfov = ifov;
            fireSelectionChanged();
        }
    }

    @Override
    public void addListener(SounderOverlayListener listener) {
        listenerMap.put(listener, null);
    }

    @Override
    public void removeListener(SounderOverlayListener listener) {
        listenerMap.remove(listener);
    }

    public int crosshairValueToChannel(double value) {
        return (int) Math.round(value * 100.0 / iDefSpectDWn1b - iDefNFirst1b + 1.0);
    }

    public double channelToCrosshairValue(int channel) {
        return (iDefSpectDWn1b * (iDefNFirst1b + channel - 1)) / 100.0;
    }

    synchronized Efov[] getEfovs() {
        if (overlayEfovs == null) {
            overlayEfovs = createEfovs();
        }
        return overlayEfovs;
    }

    protected void fireSelectionChanged() {
        final Set<SounderOverlayListener> listenerSet = listenerMap.keySet();

        synchronized (listenerMap) {
            for (final SounderOverlayListener listener : listenerSet) {
                listener.selectionChanged(this);
            }
        }
    }

    protected void fireDataChanged() {
        final Set<SounderOverlayListener> listenerSet = listenerMap.keySet();

        synchronized (listenerMap) {
            for (final SounderOverlayListener listener : listenerSet) {
                listener.dataChanged(this);
            }
        }
    }

    static int computeIfovId(int mdrIndex, int efovIndex, int ifovIndex) {
        return mdrIndex * SNOT * PN + efovIndex * PN + ifovIndex;
    }

    public static int computeMdrIndex(int ifovId) {
        return ifovId / (SNOT * PN);
    }

    public static int computeEfovIndex(int ifovId) {
        return (ifovId - computeMdrIndex(ifovId) * SNOT * PN) / PN;
    }

    public static int computeIfovIndex(int ifovId) {
        return ifovId % PN;
    }

    private Efov[] createEfovs() {
        final Efov[] newEfovs = new Efov[mdrCount * SNOT];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            try {
                readEfovMdr(mdrIndex, newEfovs, mdrIndex * SNOT);
            } catch (IOException e) {
                return Arrays.copyOfRange(newEfovs, 0, mdrIndex * SNOT);
            }
        }
        return newEfovs;
    }

    private void readEfovMdr(int mdrIndex, Efov[] efovs, int dest) throws IOException {
        CompoundData mdr = iasiFile.getMdr(mdrIndex);
        final byte mode = iasiFile.readMdrGEPSIasiMode(mdr);
        if (mode == 0) {
            final boolean[][] anomalousFlags = iasiFile.readGQisFlagQualMdr(mdr);

            for (int efovIndex = 0; efovIndex < SNOT; efovIndex++) {
                PixelPos[] ifovPos = ifovPosProvider.getIvofCenter(mdr, efovIndex);
                final Shape[] ifovShapes = ifovShapeFactory.createIfovShapes(ifovPos);
                final IasiIfov[] ifovs = new IasiIfov[PN];
                for (int ifovIndex = 0; ifovIndex < ifovs.length; ifovIndex++) {
                    final int ifovId = computeIfovId(mdrIndex, efovIndex, ifovIndex);
                    final PixelPos pos = ifovPos[ifovIndex];
                    final Shape shape = ifovShapes[ifovIndex];
                    final boolean anomalous = anomalousFlags[efovIndex][ifovIndex];

                    ifovs[ifovIndex] = new IasiIfov(ifovId, pos.x, pos.y, shape, anomalous);
                }
                final Shape efovShape = efovShapeFactory.createEfovShape(ifovs, mdr, efovIndex);
                efovs[dest + efovIndex] = new Efov(efovIndex, ifovs, efovShape);
            }
        }
    }

    private interface IfovPosProvider {
        PixelPos[] getIvofCenter(CompoundData mdr, int efovIndex) throws IOException;
    }
    
    private class AvhrrPixelPosBasedProvider implements IfovPosProvider {

        @Override
        public PixelPos[] getIvofCenter(CompoundData mdr, int efovIndex) throws IOException {
            final long[] millis = getEpsFile().readGEPSDatIasiMdr(mdr);
            final long mdrStartMillis = millis[0];
            final double[][] locs = getEpsFile().readMdrGEPSLocIasiAvhrrIASI(mdr, efovIndex);
            final PixelPos[] ifovPos = new PixelPos[PN];
            for (int ifovIndex = 0; ifovIndex < PN; ifovIndex++) {
                final double[] loc = locs[ifovIndex];
                ifovPos[ifovIndex] = calculateAvhrrPixelPos(mdrStartMillis, loc[1], loc[0]);
            }
            return ifovPos;
        }
    }
    
    private class AvhrrGeocodingBasedProvider implements IfovPosProvider {

        @Override
        public PixelPos[] getIvofCenter(CompoundData mdr, int efovIndex) throws IOException {
            final PixelPos[] ifovPos = new PixelPos[PN];
            for (int ifovIndex = 0; ifovIndex < PN; ifovIndex++) {
                final GeoPos geoPos = iasiFile.readGeoPos(mdr, efovIndex, ifovIndex);
                ifovPos[ifovIndex] = avhrrProduct.getGeoCoding().getPixelPos(geoPos, ifovPos[ifovIndex]);
            } 
            return ifovPos;
        }
    }
    
    private interface IfovShapeFactory {
        Shape[] createIfovShapes(PixelPos[] ifovPos);
    }
    
    private class EfovDistributionShapeFactory implements IfovShapeFactory {

        @Override
        public Shape[] createIfovShapes(PixelPos[] ifovPos) {
            final float scaleY01 = (ifovPos[1].y - ifovPos[0].y) / IFOV_DIST;
            final float scaleY23 = (ifovPos[2].y - ifovPos[3].y) / IFOV_DIST;

            final float xWest = 0.5f * (ifovPos[0].x + ifovPos[1].x);
            final float xEast = 0.5f * (ifovPos[2].x + ifovPos[3].x);
            final float scaleX = (xEast - xWest) / IFOV_DIST;

            final Shape[] ifovShapes = new Shape[PN];
            for (int i = 0; i < PN; i++) {
                final PixelPos pos = ifovPos[i];
                if (i < 2) {
                    ifovShapes[i] = new Ellipse2D.Float(pos.x - 0.5f * IFOV_SIZE * scaleX,
                                                        pos.y - 0.5f * IFOV_SIZE * scaleY01,
                                                        IFOV_SIZE * scaleX, IFOV_SIZE * scaleY01);
                } else {
                    ifovShapes[i] = new Ellipse2D.Float(pos.x - 0.5f * IFOV_SIZE * scaleX,
                                                        pos.y - 0.5f * IFOV_SIZE * scaleY23,
                                                        IFOV_SIZE * scaleX, IFOV_SIZE * scaleY23);
                }
            }
            return ifovShapes;
        }
    }
    
    private interface EfovShapeFactory {
        Shape createEfovShape(IasiIfov[] ifovs, CompoundData mdr, int efovIndex) throws IOException;
    }
    
    private class FastEfovShape implements EfovShapeFactory {

        @Override
        public Shape createEfovShape(IasiIfov[] ifovs, CompoundData mdr, int efovIndex) throws IOException {
            boolean started = false;
            GeneralPath path = new GeneralPath();
            for (IasiIfov ifov : ifovs) {
                if (!started) {
                    path.moveTo(ifov.getPixelX(), ifov.getPixelY());
                    started = true;
                } else {
                    path.lineTo(ifov.getPixelX(), ifov.getPixelY());
                }
            }
            path.closePath();
            return path;
        }
    }
    
    private class IisGridEfovShape implements EfovShapeFactory {

        @Override
        public Shape createEfovShape(IasiIfov[] ifovs, CompoundData mdr, int efovIndex) throws IOException {
            final double[][] iisLocs =  getEpsFile().readMdrGEPSLocIasiAvhrrIIS(mdr, efovIndex);
            final long[] millis = getEpsFile().readGEPSDatIasiMdr(mdr);
            final long mdrStartMillis = millis[0];
            
            GeneralPath path = new GeneralPath();
            final int[] start = {0, 5, 10, 15, 20, 0, 1, 2, 3, 4};
            final int[] end = {4, 9, 14, 19, 24, 20, 21, 22, 23, 24};
            for (int i = 0; i < start.length; i++) {
                double[] loc = iisLocs[start[i]];
                PixelPos pos = calculateAvhrrPixelPos(mdrStartMillis, loc[1], loc[0]);
                path.moveTo(pos.x, pos.y);

                loc = iisLocs[end[i]];
                pos = calculateAvhrrPixelPos(mdrStartMillis, loc[1], loc[0]);
                path.lineTo(pos.x, pos.y);
            }
            return path;
        }
    }
    
    private class NormanEfovShape implements EfovShapeFactory {

        @Override
        public Shape createEfovShape(IasiIfov[] ifovs, CompoundData mdr, int efovIndex) throws IOException {
            final Area area = new Area();
            boolean started = false;
            GeneralPath path = new GeneralPath();
            for (IasiIfov ifov : ifovs) {
                if (!started) {
                    path.moveTo(ifov.getPixelX(), ifov.getPixelY());
                    started = true;
                } else {
                    path.lineTo(ifov.getPixelX(), ifov.getPixelY());
                }
            }
            area.add(new Area(path));
            for (IasiIfov ifov : ifovs) {
                area.subtract(new Area(ifov.getShape()));
            }
            return area;
        }
    }
    
    private class BoundsEfovShape implements EfovShapeFactory {

        @Override
        public Shape createEfovShape(IasiIfov[] ifovs, CompoundData mdr, int efovIndex) throws IOException {
            final Area area = new Area();
            for (IasiIfov ifov : ifovs) {
                area.add(new Area(ifov.getShape()));
            }
            return area.getBounds2D();
        }
    }
    
    private class IisEfovShape implements EfovShapeFactory {

        @Override
        public Shape createEfovShape(IasiIfov[] ifovs, CompoundData mdr, int efovIndex) throws IOException {
            final double[][] iisLocs =  getEpsFile().readMdrGEPSLocIasiAvhrrIIS(mdr, efovIndex);
            final long[] millis = getEpsFile().readGEPSDatIasiMdr(mdr);
            final long mdrStartMillis = millis[0];
            double[] loc = iisLocs[0];
            GeneralPath path = new GeneralPath();
            PixelPos pos = calculateAvhrrPixelPos(mdrStartMillis, loc[1], loc[0]);
            path.moveTo(pos.x, pos.y);

            final int[] pointsIndices = {4, 24, 20};
            for (int i : pointsIndices) {
                loc = iisLocs[i];
                pos = calculateAvhrrPixelPos(mdrStartMillis, loc[1], loc[0]);
                path.lineTo(pos.x, pos.y);
            }
            path.closePath();
            return path;
        }
        
    }
    private PixelPos calculateAvhrrPixelPos(long mdrStartMillis, double locX, double locY) {
        final double u = ((mdrStartMillis - avhrrStartMillis) + locY) / (avhrrEndMillis - avhrrStartMillis);

        final float avhrrX = (float) (locX - avhrrTrimLeft);
        final float avhrrY = (float) (u * avhrrRasterHeight);

        return new PixelPos(avhrrX, avhrrY);
    }
}
