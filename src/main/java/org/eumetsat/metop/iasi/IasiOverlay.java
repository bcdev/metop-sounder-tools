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

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Debug;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.sounder.AvhrrOverlay;

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

import javax.swing.SwingWorker;

import com.bc.ceres.binio.CompoundData;


public class IasiOverlay implements AvhrrOverlay {

    // IASI record parameters
    private static final int AMCO = 100;
    private static final int AMLI = 100;
    private static final int CCD = 2;
    private static final int IMCO = 64;
    private static final int IMLI = 64;
    private static final int MAXBA = 3600;
    private static final int NBK = 6;
    private static final int NCL = 7;
    private static final int NIM = 28;
    private static final int PN = 4;
    private static final int SB = 3;
    private static final int SGI = 25;
    private static final int SNOT = 30;
    private static final int SS = 8700;
    private static final int VP = 1;
    
    private static final int IFOV_SIZE = 12;
    private static final float IFOV_DIST = 18;

    private static final Efov[] NO_DATA = new Efov[0];
    
    private final IasiFile iasiFile;
    private final Product avhrrProduct;
    private final long avhrrStartMillis;
    private final long avhrrEndMillis;
    private final int avhrrRasterHeight;
    private final int avhrrTrimLeft;
    
    private Efov[] efovs;
    private boolean computingEfovs;
    private IasiIfov selectedIfov;
    private final Map<IasiOverlayListener, Object> listenerMap;
    private int mdrCount;
    
    public IasiOverlay(IasiFile iasiFile, Product avhrrProduct) throws IOException {
        this.iasiFile = iasiFile;
        this.avhrrProduct = avhrrProduct;
        avhrrEndMillis = avhrrProduct.getEndTime().getAsCalendar().getTimeInMillis();
        avhrrStartMillis = avhrrProduct.getStartTime().getAsCalendar().getTimeInMillis();
        avhrrTrimLeft = avhrrProduct.getMetadataRoot().getElement("READER_INFO").getAttributeInt("TRIM_LEFT", 0);
        avhrrRasterHeight = avhrrProduct.getSceneRasterHeight();
        listenerMap = Collections.synchronizedMap(new WeakHashMap<IasiOverlayListener, Object>());
        mdrCount = iasiFile.getMdrCount();
    }
    
    public synchronized Efov[] getEfovs() {
        synchronized (this) {
            if (efovs != null) {
                return efovs;
            }
            if (computingEfovs) {
                return NO_DATA;
            }
            computingEfovs = true;
        }
        SwingWorker<Efov[], Object> worker = new SwingWorker<Efov[], Object>() {

            @Override
            protected Efov[] doInBackground() throws Exception {
                return createEfovs("fast");
            }
                
            @Override
            protected void done() {
                try {
                    synchronized (IasiOverlay.this) {
                        computingEfovs = false;
                        efovs = get();
                    }
                    fireDataChanged();
                } catch (Exception e) {
                    Debug.trace(e);
                }
            }
        };
        worker.execute();
        return NO_DATA;
    }
    
    public IasiIfov getSelectedIfov() {
        return selectedIfov;
    }
    
    public void setSelectedIfov(IasiIfov selectedIfov) {
        if (selectedIfov != this.selectedIfov) {
            this.selectedIfov = selectedIfov;
            fireSelectionChanged();
        }
    }
    
    public void addListener(IasiOverlayListener listener) {
        listenerMap.put(listener, null);
    }

    public void removeListener(IasiOverlayListener listener) {
        listenerMap.remove(listener);
    }

    protected void fireSelectionChanged() {
        final Set<IasiOverlayListener> listenerSet = listenerMap.keySet();

        synchronized (listenerMap) {
            for (final IasiOverlayListener listener : listenerSet) {
                listener.selectionChanged(this);
            }
        }
    }
    
    protected void fireDataChanged() {
        final Set<IasiOverlayListener> listenerSet = listenerMap.keySet();

        synchronized (listenerMap) {
            for (final IasiOverlayListener listener : listenerSet) {
                listener.dataChanged(this);
            }
        }
    }

    public Product getAvhrrProduct() {
        return avhrrProduct;
    }

    public EpsFile getEpsFile() {
        return iasiFile;
    }
    
    public IasiFile getIasiFile() {
        return iasiFile;
    }
    
    public void close() {
        iasiFile.close();
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
    
    private Efov[] createEfovs(String efovStyle) {
        final Efov[] newEfovs = new Efov[mdrCount * SNOT];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            try {
                readEfovMdr(mdrIndex, efovStyle, newEfovs, mdrIndex * SNOT);
            } catch (IOException e) {
                    return Arrays.copyOfRange(newEfovs, 0, mdrIndex * SNOT);
            }
        }
        return newEfovs;
    }
    
    private void readEfovMdr(int mdrIndex, String efovStyle, Efov[] efovs, int dest) throws IOException {
        final double[][][] locs;
        final double[][][] iisLocs;
        final long[] millis;
        final boolean[][] anomalousFlags;
        final byte mode;
        CompoundData mdr = iasiFile.getMdr(mdrIndex);
        locs = iasiFile.readMdrGEPSLocIasiAvhrrIASI(mdr);
        millis = iasiFile.readGEPSDatIasiMdr(mdr);
        anomalousFlags = iasiFile.readGQisFlagQualMdr(mdr);
        mode = iasiFile.readMdrGEPSIasiMode(mdr);
        // todo - to Efov shape factory
        iisLocs = iasiFile.readMdrGEPSLocIasiAvhrrIIS(mdr);

        final long mdrStartMillis = millis[0];

        if (mode == 0) {
            for (int efovIndex = 0; efovIndex < SNOT; efovIndex++) {
                final PixelPos[] ifovPos = new PixelPos[PN];

                for (int ifovIndex = 0; ifovIndex < PN; ifovIndex++) {
                    final double[] loc = locs[efovIndex][ifovIndex];
                    ifovPos[ifovIndex] = calculateAvhrrPixelPos(mdrStartMillis, loc[1], loc[0]);
                }
                final Shape[] ifovShapes = createIfovShapes(ifovPos);
                final IasiIfov[] ifovs = new IasiIfov[PN];

                for (int ifovIndex = 0; ifovIndex < ifovs.length; ifovIndex++) {
                    final int ifovId = computeIfovId(mdrIndex, efovIndex, ifovIndex);
                    final PixelPos pos = ifovPos[ifovIndex];
                    final Shape shape = ifovShapes[ifovIndex];
                    final boolean anomalous = anomalousFlags[efovIndex][ifovIndex];

                    ifovs[ifovIndex] = new IasiIfov(ifovId, pos.x, pos.y, shape, anomalous);
                }

                final Shape efovShape = createEfovShape(efovStyle, mdrStartMillis, iisLocs[efovIndex], ifovs);
                efovs[dest + efovIndex] = new Efov(efovIndex, ifovs, efovShape);
            }
        }
    }


    // todo - to Ifov shape factory
    private Shape[] createIfovShapes(PixelPos[] ifovPos) {
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

    // todo - to Efov shape factory
    private Shape createEfovShape(String efovStyle, long mdrStartMillis, double[][] iisLocs, IasiIfov[] ifovs) {
        if ("grid".equals(efovStyle)) {
            return createIisGridShape(mdrStartMillis, iisLocs);
        } else if ("bounds".equals(efovStyle)) {
            return createEfovBoundShape(ifovs);
        } else if ("norman".equals(efovStyle)) {
            return createNormanShape(ifovs);
        } else if ("fast".equals(efovStyle)) {
            return createFastShape(ifovs);
        }
        return createIisEfovShape(mdrStartMillis, iisLocs);
    }

    private Shape createFastShape(IasiIfov[] ifovs) {
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

    private Shape createIisGridShape(long mdrStartMillis, double[][] iisLocs) {
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
    
    // todo - to Efov shape factory
    private Shape createNormanShape(IasiIfov[] ifovs) {
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

    // todo - to Efov shape factory
    private Shape createEfovBoundShape(IasiIfov[] ifovs) {
        final Area area = new Area();
        for (IasiIfov ifov : ifovs) {
            area.add(new Area(ifov.getShape()));
        }
        return area.getBounds2D();
    }

    
    // todo - to Efov shape factory
    private Shape createIisEfovShape(long mdrStartMillis, double[][] iisLocs) {
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

    private PixelPos calculateAvhrrPixelPos(long mdrStartMillis, double locX, double locY) {
        final double u = ((mdrStartMillis - avhrrStartMillis) + locY) / (avhrrEndMillis - avhrrStartMillis);

        final float avhrrX = (float) (locX - avhrrTrimLeft);
        final float avhrrY = (float) (u * avhrrRasterHeight);

        return new PixelPos(avhrrX, avhrrY);
    }
}
