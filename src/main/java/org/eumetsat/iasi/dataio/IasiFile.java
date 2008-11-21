/*
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.iasi.dataio;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @author Marco Z�hlke
 * @version $Revision: 82 $ $Date: 2008-01-10 16:19:24 +0100 (Do, 10 Jan 2008) $
 */
public class IasiFile {

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

    public static final int IFOV_SIZE = 12;

    public static final float IFOV_DIST = 18;

    private static final int G_EPS_IASI_MODE_OFFSET = 22;
    private static final int G_EPS_LOC_IASI_AVHRR_IASI_OFFSET = 62;
    private static final int G_EPS_LOC_IASI_AVHRR_IIS_OFFSET = 1262;
    private static final int G_EPS_DAT_IASI_OFFSET = 9122;
    private static final int G_QIS_FLAG_QUAL_OFFSET = 255260;
    private static final int G_GEO_SOND_LOC_OFFSET = 255413;
    private static final int G_GEO_SOND_ANGLES_METOP = 256373;
    private static final int G_GEO_SOND_ANGLES_SUN = 263333;
    private static final int I_DEF_SPECT_DWN_1B_OFFSET = 276297;

    //    private static final int I_DEF_NSFIRST_1B_OFFSET = 276302;
    private static final int G_S1C_SPECT_OFFSET = 276310;
    private static final double G_GEO_SOND_LOC_SCALING_FACTOR = 1.0E-6;

    private final File file;
    private final long avhrrStartMillis;
    private final long avhrrEndMillis;
    private final int avhrrTrimLeft;
    private final int avhrrRasterHeight;
    private final ImageInputStream iis;
    private MainProductHeaderRecord mainProductHeaderRecord;
    private GiadrScaleFactors giadrScaleFactors;

    private long firstMdrOffset;

    private int mdrCount;
    private long mdrSize;
    private Efov[] efovs;

    public IasiFile(File file, long avhrrStartMillis, long avhrrEndMillis, int avhrrRasterHeight,
                    int avhrrTrimLeft) throws IOException {
        this.file = file;
        iis = new FileImageInputStream(file);
        this.avhrrStartMillis = avhrrStartMillis;
        this.avhrrEndMillis = avhrrEndMillis;
        this.avhrrRasterHeight = avhrrRasterHeight;
        this.avhrrTrimLeft = avhrrTrimLeft;

        readHeader();
    }

    public File getFile() {
        return file;
    }


    public synchronized Efov[] getEfovs() {
        if (efovs == null) {
            System.out.println("createing EFOVs");
            efovs = createEfovs("norman");
        }

        return efovs;
    }

    public Ifov getIfov(int index) {
        return getEfovs()[index / PN].getIfovs()[computeIfovIndex(index)];
    }

    public Ifov readIfovMdr(int ifovId) {
        final Efov[] efovs = new Efov[SNOT];
        try {
            readEfovMdr(computeMdrIndex(ifovId), "norman", efovs, 0);
            final int efovInMdrIndex = ifovId % (SNOT);
            return efovs[efovInMdrIndex].getIfovs()[computeIfovIndex(ifovId)];
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void close() {
        try {
            iis.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static boolean isIasiFile(File file) throws IOException {
        final ImageInputStream iis = new FileImageInputStream(file);

        try {
            final GenericRecordHeader mphrHeader = GenericRecordHeader.readGenericRecordHeader(iis);

            // todo - look for IASI-specific entries in MPHR?

            iis.seek(mphrHeader.recordSize);
            final GenericRecordHeader iprHeader = GenericRecordHeader.readGenericRecordHeader(iis);

            // todo - look for IASI-specific records in IPR?

            return mphrHeader.recordClass == RecordClass.MPHR
                    && mphrHeader.instrumentGroup == InstrumentGroup.GENERIC
                    && mphrHeader.recordSubclass == 0
                    && iprHeader.recordClass == RecordClass.IPR
                    && iprHeader.instrumentGroup == InstrumentGroup.GENERIC
                    && iprHeader.recordSubclass == 0;
        } finally {
            try {
                iis.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public void readHeader() throws IOException {
        final GenericRecordHeader mphrHeader = GenericRecordHeader.readGenericRecordHeader(iis);

        if (mphrHeader.recordClass != RecordClass.MPHR
                || mphrHeader.instrumentGroup != InstrumentGroup.GENERIC
                || mphrHeader.recordSubclass != 0) {
            throw new IOException("Illegal Main Product Header Record");
        }

        mainProductHeaderRecord = new MainProductHeaderRecord();
        mainProductHeaderRecord.readRecord(iis);

        final List<InternalPointerRecord> iprList = new ArrayList<InternalPointerRecord>();
        for (; ;) {
            final InternalPointerRecord ipr = InternalPointerRecord.readInternalPointerRecord(iis);
            iprList.add(ipr);
            if (ipr.targetRecordClass == RecordClass.MDR) {
                break;
            }
        }

        for (final InternalPointerRecord ipr : iprList) {
            if (ipr.targetRecordClass == RecordClass.GIADR) {
                if (ipr.targetRecordSubclass == 0) {
                    iis.seek(ipr.targetRecordOffset);
                    GiadrQuality giadrQuality = new GiadrQuality();
                    giadrQuality.readRecord(iis);
                } else if (ipr.targetRecordSubclass == 1) {
                    iis.seek(ipr.targetRecordOffset);
                    giadrScaleFactors = new GiadrScaleFactors();
                    giadrScaleFactors.readRecord(iis);
                }
            } else if (ipr.targetRecordClass == RecordClass.MDR) {
                firstMdrOffset = ipr.targetRecordOffset;
            }
        }

        determineMdrCount(iis);
    }

    private Efov[] createEfovs(String efovStyle) {
        final Efov[] efovs = new Efov[mdrCount * SNOT];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            try {
                readEfovMdr(mdrIndex, efovStyle, efovs, mdrIndex * SNOT);
            } catch (IOException e) {
                    return Arrays.copyOfRange(efovs, 0, mdrIndex * SNOT);
            }
        }

        return efovs;
    }

    private void readEfovMdr(int mdrIndex, String efovStyle, Efov[] efovs, int dest) throws IOException {
        final double[][][] locs;
        final double[][][] iisLocs;
        final long[] millis;
        final boolean[][] anomalousFlags;
        final byte mode;
        locs = readMdrGEPSLocIasiAvhrrIASI(mdrIndex);
        millis = readGEPSDatIasiMdr(mdrIndex);
        anomalousFlags = readGQisFlagQualMdr(mdrIndex);
        mode = readMdrGEPSIasiMode(mdrIndex);
        // todo - to Efov shape factory
        iisLocs = readMdrGEPSLocIasiAvhrrIIS(mdrIndex);

        final long mdrStartMillis = millis[0];

        if (mode == 0) {
            for (int efovIndex = 0; efovIndex < SNOT; efovIndex++) {
                final PixelPos[] ifovPos = new PixelPos[PN];

                for (int ifovIndex = 0; ifovIndex < PN; ifovIndex++) {
                    final double[] loc = locs[efovIndex][ifovIndex];
                    ifovPos[ifovIndex] = calculateAvhrrPixelPos(mdrStartMillis, loc[1], loc[0]);
                }
                final Shape[] ifovShapes = createIfovShapes(ifovPos);
                final Ifov[] ifovs = new Ifov[PN];

                for (int ifovIndex = 0; ifovIndex < ifovs.length; ifovIndex++) {
                    final int ifovId = computeIfovId(mdrIndex, efovIndex, ifovIndex);
                    final PixelPos pos = ifovPos[ifovIndex];
                    final Shape shape = ifovShapes[ifovIndex];
                    final boolean anomalous = anomalousFlags[efovIndex][ifovIndex];

                    ifovs[ifovIndex] = new Ifov(ifovId, pos.x, pos.y, shape, anomalous);
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
    private Shape createEfovShape(String efovStyle, long mdrStartMillis, double[][] iisLocs, Ifov[] ifovs) {
        if ("grid".equals(efovStyle)) {
            return createIisGridShape(mdrStartMillis, iisLocs);
        } else if ("bounds".equals(efovStyle)) {
            return createEfovBoundShape(ifovs);
        } else if ("norman".equals(efovStyle)) {
            return createNormanShape(ifovs);
        }
        return createIisEfovShape(mdrStartMillis, iisLocs);
    }

    // todo - to Efov shape factory
    private Shape createNormanShape(Ifov[] ifovs) {
        final Area area = new Area();
        boolean started = false;
        GeneralPath path = new GeneralPath();
        for (Ifov ifov : ifovs) {
            if (!started) {
                path.moveTo(ifov.getPixelX(), ifov.getPixelY());
                started = true;
            } else {
                path.lineTo(ifov.getPixelX(), ifov.getPixelY());
            }
        }
        area.add(new Area(path));
        for (Ifov ifov : ifovs) {
            area.subtract(new Area(ifov.getShape()));
        }
        return area;
    }

    // todo - to Efov shape factory
    private Shape createEfovBoundShape(Ifov[] ifovs) {
        final Area area = new Area();
        for (Ifov ifov : ifovs) {
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

    private PixelPos calculateAvhrrPixelPos(long mdrStartMillis, double locX, double locY) {
        final double u = ((mdrStartMillis - avhrrStartMillis) + locY) / (avhrrEndMillis - avhrrStartMillis);

        final float avhrrX = (float) (locX - avhrrTrimLeft);
        final float avhrrY = (float) (u * avhrrRasterHeight);

        return new PixelPos(avhrrX, avhrrY);
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

    private void determineMdrCount(ImageInputStream iis) throws IOException {
        final GenericRecordHeader mdrHeader;

        synchronized (this.iis) {
            iis.seek(firstMdrOffset);
            mdrHeader = GenericRecordHeader.readGenericRecordHeader(iis);
        }

        mdrSize = mdrHeader.recordSize;
        mdrCount = (int) ((iis.length() - firstMdrOffset) / mdrSize);
    }

    private long getMdrOffset(int i) {
        return firstMdrOffset + (i * mdrSize);
    }

    public GeoPos readGeoPos(int ifovId) throws IOException {
        final long mdrOffset = getMdrOffset(computeMdrIndex(ifovId));
        final int efovIndex = computeEfovIndex(ifovId);
        final int ifovIndex = computeIfovIndex(ifovId);

        final float lon;
        final float lat;
        synchronized (iis) {
            iis.seek(mdrOffset + G_GEO_SOND_LOC_OFFSET + (efovIndex * PN + ifovIndex) * 2 * 4);
            lon = (float) (iis.readInt() * G_GEO_SOND_LOC_SCALING_FACTOR);
            lat = (float) (iis.readInt() * G_GEO_SOND_LOC_SCALING_FACTOR);
        }
        return new GeoPos(lat, lon);
    }

    public double[][][][] readGGeoSondLoc() throws IOException {
        final double[][][][] data = new double[mdrCount][SNOT][PN][2];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            final long mdrOffset = getMdrOffset(mdrIndex);
            final int[] mdrBlock = new int[SNOT * PN * 2];

            synchronized (iis) {
                iis.seek(mdrOffset + G_GEO_SOND_LOC_OFFSET);
                iis.readFully(mdrBlock, 0, mdrBlock.length);
            }

            for (int i = 0, j = 0; j < SNOT; j++) {
                for (int k = 0; k < PN; k++) {
                    data[mdrIndex][j][k][0] = mdrBlock[i++] * G_GEO_SOND_LOC_SCALING_FACTOR;
                    data[mdrIndex][j][k][1] = mdrBlock[i++] * G_GEO_SOND_LOC_SCALING_FACTOR;
                }
            }
        }

        return data;
    }

    public double[][][][] readGEPSLocIasiAvhrrIASI() throws IOException {
        final double[][][][] data = new double[mdrCount][][][];

        for (int i = 0; i < mdrCount; i++) {
            data[i] = readMdrGEPSLocIasiAvhrrIASI(i);
        }

        return data;
    }

    private double[][][] readMdrGEPSLocIasiAvhrrIASI(int mdrIndex) throws IOException {
        final double[][][] data = new double[SNOT][PN][2];
        final long mdrOffset = getMdrOffset(mdrIndex);

        synchronized (iis) {
            iis.seek(mdrOffset + G_EPS_LOC_IASI_AVHRR_IASI_OFFSET);

            for (int j = 0; j < SNOT; j++) {
                for (int k = 0; k < PN; k++) {
                    for (int l = 0; l < 2; l++) {
                        data[j][k][l] = EpsMetopUtil.readVInt4(iis);
                    }
                }
            }
        }
        return data;
    }

    public double[][][][] readGEPSLocIasiAvhrrIIS() throws IOException {
        final double[][][][] data = new double[mdrCount][][][];

        for (int i = 0; i < mdrCount; i++) {
            data[i] = readMdrGEPSLocIasiAvhrrIIS(i);
        }

        return data;
    }

    private double[][][] readMdrGEPSLocIasiAvhrrIIS(int mdrIndex) throws IOException {
        final long mdrOffset = getMdrOffset(mdrIndex);
        final double[][][] data = new double[SNOT][SGI][2];

        synchronized (iis) {
            iis.seek(mdrOffset + G_EPS_LOC_IASI_AVHRR_IIS_OFFSET);

            for (int j = 0; j < SNOT; j++) {
                for (int k = 0; k < SGI; k++) {
                    for (int l = 0; l < 2; l++) {
                        data[j][k][l] = EpsMetopUtil.readVInt4(iis);
                    }
                }
            }
        }
        return data;
    }

    public long[][] readGEPSDatIasi() throws IOException {
        final long[][] data = new long[mdrCount][];

        for (int i = 0; i < mdrCount; i++) {
            data[i] = readGEPSDatIasiMdr(i);
        }

        return data;
    }

    public long[] readGEPSDatIasiMdr(int mdrIndex) throws IOException {
        final long[] data = new long[SNOT];
        final long mdrOffset = getMdrOffset(mdrIndex);

        synchronized (iis) {
            iis.seek(mdrOffset + G_EPS_DAT_IASI_OFFSET);

            for (int j = 0; j < SNOT; j++) {
                data[j] = EpsMetopUtil.readShortCdsTime(iis).getAsCalendar().getTimeInMillis();
            }
        }
        return data;
    }

    public ProductData.UTC readGEPSDatIasi(int ifovIndex) throws IOException {
        final int efovIndex = ifovIndex / PN;
        final long mdrOffset = getMdrOffset(efovIndex / SNOT);
        final long efovOffset = (efovIndex % SNOT) * 6;
        final ProductData.UTC utc;

        synchronized (iis) {
            iis.seek(efovOffset + mdrOffset + G_EPS_DAT_IASI_OFFSET);
            utc = EpsMetopUtil.readShortCdsTime(iis);
        }

        return utc;
    }

    public boolean[][][] readGQisFlagQual() throws IOException {
        final boolean[][][] data = new boolean[mdrCount][][];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            data[mdrIndex] = readGQisFlagQualMdr(mdrIndex);
        }

        return data;
    }

    private boolean[][] readGQisFlagQualMdr(int mdrIndex) throws IOException {
        final long mdrOffset = getMdrOffset(mdrIndex);
        final boolean[][] data = new boolean[SNOT][PN];
        final byte[] mdrBlock = new byte[SNOT * PN];

        synchronized (iis) {
            iis.seek(mdrOffset + G_QIS_FLAG_QUAL_OFFSET);
            iis.readFully(mdrBlock, 0, mdrBlock.length);
        }

        for (int i = 0, j = 0; j < SNOT; j++) {
            for (int k = 0; k < PN; k++) {
                data[j][k] = mdrBlock[i++] != 0;
            }
        }
        return data;

    }

    public boolean readGQisFlagQual(int ifovId) throws IOException {
        final long mdrOffset = getMdrOffset(computeMdrIndex(ifovId));
        final int efovIndex = computeEfovIndex(ifovId);
        final int ifovIndex = computeIfovIndex(ifovId);
        final int ifovOffset = (efovIndex * PN) + ifovIndex;

        final boolean anomalous;

        synchronized (iis) {
            iis.seek(mdrOffset + G_QIS_FLAG_QUAL_OFFSET + ifovOffset);
            anomalous = iis.readBoolean();
        }

        return anomalous;
    }

    public byte[] readGEPSIasiMode() throws IOException {
        final byte[] modes = new byte[mdrCount];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            modes[mdrIndex] = readMdrGEPSIasiMode(mdrIndex);
        }

        return modes;
    }

    private byte readMdrGEPSIasiMode(int mdrIndex) throws IOException {
        final long mdrOffset = getMdrOffset(mdrIndex);

        synchronized (iis) {
            iis.seek(mdrOffset + G_EPS_IASI_MODE_OFFSET + 2);
            return iis.readByte();
        }
    }
    
    public double[][][] readAllBts(int channelId) throws IOException {
        final double[][][] data = new double[mdrCount][][];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            data[mdrIndex] = readAllBts(channelId, mdrIndex);
        }
        return data;
    }
    
    private double[][] readAllBts(int channelId, int mdrIndex) throws IOException {
        final long mdrOffset = getMdrOffset(mdrIndex);
        
        final double[][] data = new double[SNOT][PN];
        final double iDefSpectDWn1b;
        final int iDefNsFirst1b;

        synchronized (iis) {
            iis.seek(mdrOffset + I_DEF_SPECT_DWN_1B_OFFSET);
            iDefSpectDWn1b = EpsMetopUtil.readVInt4(iis);
            iDefNsFirst1b = iis.readInt();
        }
        final double[] scaleFactors = giadrScaleFactors.getScaleFactors(iDefNsFirst1b);
        final double wavenumber = iDefSpectDWn1b * (iDefNsFirst1b + channelId - 1);
        
        for (int j = 0; j < SNOT; j++) {
            for (int k = 0; k < PN; k++) {
                final int spectrumOffset = ((j * PN) + k) * SS * 2;
                synchronized (iis) {
                    iis.seek(mdrOffset + G_S1C_SPECT_OFFSET + spectrumOffset + 2 * channelId);
                    short spectrumSample = iis.readShort();
                    final double radiance = scaleFactors[channelId] * spectrumSample;
                    double bt = brightnessTemperature(wavenumber, radiance);
                    data[j][k] = bt;
                }
            }
        }
        return data;
    }
    
    private static double brightnessTemperature(double wavenumber, double radiance) {
        final double c1 = 1.191042722E-16;
        final double c2 = 1.4387752E-2;
        final double a = c2 * wavenumber;
        final double b = c1 * wavenumber * wavenumber * wavenumber;

        return a / (Math.log(1.0 + (b / radiance)));
    }

    public double[][] readSpectrum(int ifovId) throws IOException {
        final long mdrOffset = getMdrOffset(computeMdrIndex(ifovId));

        final int efovIndex = computeEfovIndex(ifovId);
        final int ifovIndex = computeIfovIndex(ifovId);

        final int spectrumOffset = ((efovIndex * PN) + ifovIndex) * SS * 2;

        final double iDefSpectDWn1b;
        final int iDefNsFirst1b;
        final int iDefNsLast1b;

        synchronized (iis) {
            iis.seek(mdrOffset + I_DEF_SPECT_DWN_1B_OFFSET);
            iDefSpectDWn1b = EpsMetopUtil.readVInt4(iis);
            iDefNsFirst1b = iis.readInt();
            iDefNsLast1b = iis.readInt();
        }

        final int spectrumSize = iDefNsLast1b - iDefNsFirst1b + 1;
        final short[] rawSpectrum = new short[SS];
        final double[][] spectrum = new double[spectrumSize][2];

        synchronized (iis) {
            iis.seek(mdrOffset + G_S1C_SPECT_OFFSET + spectrumOffset);
            iis.readFully(rawSpectrum, 0, SS);
        }
        final double[] scaleFactors = giadrScaleFactors.getScaleFactors(iDefNsFirst1b);

        for (int i = 0; i < spectrumSize; i++) {
            final double wavenumber = iDefSpectDWn1b * (iDefNsFirst1b + i - 1);
            final double radiance = scaleFactors[i] * rawSpectrum[i];

            spectrum[i][0] = wavenumber;
            spectrum[i][1] = radiance;
        }

        return spectrum;
    }

    public static class RadianceAnalysis {
        public final int[] channels = new int[NBK];
        public int nbClass;
        public final double[] wgt = new double[NCL];
        public final double[] y = new double[NCL];
        public final double[] z = new double[NCL];
        public final double[][] mean = new double[NCL][NBK];
        public final double[][] std = new double[NCL][NBK];
        public final byte[][] image = new byte[AMLI][AMCO];
        public int mode;
        public int imageW;
        public int imageH;
        public double avhrrY;
        public double avhrrX;
        public byte type;
    }

    public RadianceAnalysis readRadianceAnalysis(int ifovId) throws IOException {
        final long mdrOffset = getMdrOffset(computeMdrIndex(ifovId));

        final int efovIndex = computeEfovIndex(ifovId);
        final int ifovIndex = computeIfovIndex(ifovId);
        final int numIfovs = efovIndex * PN + ifovIndex;

        final RadianceAnalysis radianceAnalysis = new RadianceAnalysis();

        synchronized (iis) {
            iis.seek(mdrOffset + 2365310);
            iis.readFully(radianceAnalysis.channels, 0, radianceAnalysis.channels.length);

            iis.seek(mdrOffset + 2365334 + numIfovs * 4);
            radianceAnalysis.nbClass = iis.readInt();

            iis.seek(mdrOffset + 2365814 + numIfovs * NCL * 5);
            for (int i = 0; i < NCL; i++) {
                radianceAnalysis.wgt[i] = EpsMetopUtil.readVInt4(iis);
            }

            iis.seek(mdrOffset + 2370014 + numIfovs * NCL * 4);
            for (int i = 0; i < NCL; i++) {
                radianceAnalysis.y[i] = iis.readInt() * 1E-6;
            }

            iis.seek(mdrOffset + 2373374 + numIfovs * NCL * 4);
            for (int i = 0; i < NCL; i++) {
                radianceAnalysis.z[i] = iis.readInt() * 1E-6;
            }

            iis.seek(mdrOffset + 2376734 + numIfovs * NCL * NBK * 5);
            for (int i = 0; i < NCL; i++) {
                for (int j = 0; j < NBK; j++) {
                    radianceAnalysis.mean[i][j] = EpsMetopUtil.readVInt4(iis);
                }
            }
            iis.seek(mdrOffset + 2401934 + numIfovs * NCL * NBK * 5);
            for (int i = 0; i < NCL; i++) {
                for (int j = 0; j < NBK; j++) {
                    radianceAnalysis.std[i][j] = EpsMetopUtil.readVInt4(iis);
                }
            }
            iis.seek(mdrOffset + 2427134 + efovIndex * AMLI * AMCO);
            for (int i = 0; i < AMLI; i++) {
                for (int j = 0; j < AMCO; j++) {
                    radianceAnalysis.image[i][j] = iis.readByte();
                }
            }
            iis.seek(mdrOffset + 2727134);
            radianceAnalysis.mode = iis.readInt();
            iis.seek(mdrOffset + 2727138 + efovIndex * 2);
            radianceAnalysis.imageH = iis.readShort();
            iis.seek(mdrOffset + 2727198 + efovIndex * 2);
            radianceAnalysis.imageW = iis.readShort();
            iis.seek(mdrOffset + 2727258 + efovIndex * 5);
            radianceAnalysis.avhrrY = EpsMetopUtil.readVInt4(iis);
            iis.seek(mdrOffset + 2727408 + efovIndex * 5);
            radianceAnalysis.avhrrX = EpsMetopUtil.readVInt4(iis);
            iis.seek(mdrOffset + 2727558);
            radianceAnalysis.type = iis.readByte();
        }
        
        return radianceAnalysis;
    }

    public static class Geometry {
        public double vza;
        public double sza;
        public double vaa;
        public double saa;
    }

    public Geometry readGeometry(int ifovId) throws IOException {
        final long mdrOffset = getMdrOffset(computeMdrIndex(ifovId));
        final int efovIndex = computeEfovIndex(ifovId);
        final int ifovIndex = computeIfovIndex(ifovId);

        Geometry geometry = new Geometry();
        synchronized (iis) {
            iis.seek(mdrOffset + G_GEO_SOND_ANGLES_METOP + (efovIndex * PN + ifovIndex) * 2 * 4);
            geometry.vza = iis.readInt() * G_GEO_SOND_LOC_SCALING_FACTOR;
            geometry.vaa = iis.readInt() * G_GEO_SOND_LOC_SCALING_FACTOR;

            iis.seek(mdrOffset + G_GEO_SOND_ANGLES_SUN + (efovIndex * PN + ifovIndex) * 2 * 4);
            geometry.sza = iis.readInt() * G_GEO_SOND_LOC_SCALING_FACTOR;
            geometry.saa = iis.readInt() * G_GEO_SOND_LOC_SCALING_FACTOR;
        }
        return geometry;
    }

    public static File findIasiFile(long avhrrStartTime, File[] files) {
        try {
            long leastTimeDifference = Long.MAX_VALUE;
            int index = -1;

            for (int i = 0; i < files.length; i++) {
                final long iasiStartTime = extractStartTimeInMillis(files[i].getName());
                final long timeDifference = avhrrStartTime - iasiStartTime;

                if (timeDifference > 0 && timeDifference < leastTimeDifference) {
                    leastTimeDifference = timeDifference;
                    index = i;
                }
            }
            if (index != -1) {
                return files[index];
            } else {
                return null;
            }
        } catch (ParseException e) {
            return null;
        }
    }

    public static long extractStartTimeInMillis(String filename) throws ParseException {
        if (filename.length() < 30) {
            throw new IllegalArgumentException("filename.length < 30");
        }
        final String timeString = filename.substring(16, 30);
        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.parse(timeString).getTime();
    }


    public static class IasiFilenameFilter implements FilenameFilter {

        private final String iasiFilenamePrefix;

        public IasiFilenameFilter(String avhrrFilename) {
            iasiFilenamePrefix = "IASI" + avhrrFilename.substring(4, 15).replace("1B", "1C");
        }

        public boolean accept(File dir, String name) {
            return name.startsWith(iasiFilenamePrefix) && name.endsWith(".nat");
        }
    }
}
