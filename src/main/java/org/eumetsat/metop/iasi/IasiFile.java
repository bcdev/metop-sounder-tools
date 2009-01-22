/*
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.metop.iasi;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.glayer.Layer;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.sounder.AvhrrOverlay;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;


public class IasiFile extends EpsFile {

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
    private static final double G_GEO_SOND_LOC_SCALING_FACTOR = 1.0E-6;

    private GiadrScaleFactors giadrScaleFactors;
    private int mdrCount;
    
    private SequenceData mdrSequence;

    public IasiFile(File file, DataFormat dataFormat) throws IOException {
        super(file, dataFormat);
        readHeader();
    }

    @Override
    public boolean hasOverlayFor(Product avhrrProduct) {
        // TODO check for date
        return true;
    }
  
    @Override
    public AvhrrOverlay createOverlay(Product avhrrProduct) {
      // TODO check for date
        try {
            return new IasiOverlay(this, avhrrProduct);
        } catch (IOException e) {
            return null;
        }
    }
  
    @Override
    public Layer createLayer(AvhrrOverlay overlay) {
        IasiOverlay iasiOverlay = (IasiOverlay) overlay;
        return new IasiLayer(iasiOverlay);
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
    
    private void readHeader() throws IOException {
        CompoundData giadrScaleFactorsRecord = getAuxDataRecord("giadr:iasi:1");
        giadrScaleFactors = new GiadrScaleFactors(giadrScaleFactorsRecord);
        
        mdrSequence = getMdrData();
        mdrCount = getMdrCount();
    }
    

    public CompoundData getMdr(int mdrIndex) throws IOException {
        return mdrSequence.getCompound(mdrIndex).getCompound(1);
    }

    public GeoPos readGeoPos(int ifovId) throws IOException {
        final int mdrIndex = computeMdrIndex(ifovId);
        final int efovIndex = computeEfovIndex(ifovId);
        final int ifovIndex = computeIfovIndex(ifovId);

        CompoundData mdrBody = mdrSequence.getCompound(mdrIndex).getCompound(1);
        SequenceData geoSondLoc = mdrBody.getSequence("GGeoSondLoc");
        SequenceData lonLatSequence = geoSondLoc.getSequence(efovIndex).getSequence(ifovIndex);
        final float lon = (float) (lonLatSequence.getInt(0) * G_GEO_SOND_LOC_SCALING_FACTOR);
        final float lat = (float) (lonLatSequence.getInt(1) * G_GEO_SOND_LOC_SCALING_FACTOR);
        return new GeoPos(lat, lon);
    }

    public double[][][][] readGGeoSondLoc() throws IOException {
        final double[][][][] data = new double[mdrCount][SNOT][PN][2];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            SequenceData geoSondLoc = getMdr(mdrIndex).getSequence("GGeoSondLoc");

            for (int j = 0; j < SNOT; j++) {
                SequenceData efovSequence = geoSondLoc.getSequence(j);
                for (int k = 0; k < PN; k++) {
                    SequenceData lonLatSequence = efovSequence.getSequence(k);
                    data[mdrIndex][j][k][0] = lonLatSequence.getInt(0) * G_GEO_SOND_LOC_SCALING_FACTOR;
                    data[mdrIndex][j][k][1] = lonLatSequence.getInt(1) * G_GEO_SOND_LOC_SCALING_FACTOR;
                }
            }
        }
        return data;
    }

    public double[][][][] readGEPSLocIasiAvhrrIASI() throws IOException {
        final double[][][][] data = new double[mdrCount][][][];

        for (int i = 0; i < mdrCount; i++) {
            CompoundData mdr = getMdr(i);
            data[i] = readMdrGEPSLocIasiAvhrrIASI(mdr);
        }
        return data;
    }

    double[][][] readMdrGEPSLocIasiAvhrrIASI(CompoundData mdr) throws IOException {
        final double[][][] data = new double[SNOT][PN][2];
        SequenceData mdrData = mdr.getSequence("GEPSLocIasiAvhrr_IASI");

        for (int j = 0; j < SNOT; j++) {
            SequenceData efovData = mdrData.getSequence(j);
            for (int k = 0; k < PN; k++) {
                SequenceData ifovData = efovData.getSequence(k);
                for (int l = 0; l < 2; l++) {
                    data[j][k][l] = EpsFile.readVInt4(ifovData.getCompound(l));
                }
            }
        }
        return data;
    }

    double[][][] readMdrGEPSLocIasiAvhrrIIS(CompoundData mdr) throws IOException {
        final double[][][] data = new double[SNOT][SGI][2];
        SequenceData mdrData = mdr.getSequence("GEPSLocIasiAvhrr_IIS");
        
        for (int j = 0; j < SNOT; j++) {
            SequenceData efovData = mdrData.getSequence(j);
            for (int k = 0; k < SGI; k++) {
                SequenceData ifovData = efovData.getSequence(k);
                for (int l = 0; l < 2; l++) {
                    data[j][k][l] = EpsFile.readVInt4(ifovData.getCompound(l));
                }
            }
        }
        return data;
    }

    long[] readGEPSDatIasiMdr(CompoundData mdr) throws IOException {
        final long[] data = new long[SNOT];
        SequenceData mdrData = mdr.getSequence("GEPSDatIasi");

        for (int j = 0; j < SNOT; j++) {
            CompoundData efovData = mdrData.getCompound(j);
            UTC shortCdsTime = EpsFile.readShortCdsTime(efovData);
            data[j] = shortCdsTime.getAsCalendar().getTimeInMillis();
        }
        return data;
    }

    boolean[][][] readGQisFlagQual() throws IOException {
        final boolean[][][] data = new boolean[mdrCount][][];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            CompoundData mdr = getMdr(mdrIndex);
            data[mdrIndex] = readGQisFlagQualMdr(mdr);
        }

        return data;
    }

    boolean[][] readGQisFlagQualMdr(CompoundData mdr) throws IOException {
        final boolean[][] data = new boolean[SNOT][PN];

        SequenceData mdrData = mdr.getSequence("GQisFlagQual");
        
        for (int j = 0; j < SNOT; j++) {
            SequenceData efovData = mdrData.getSequence(j);
            for (int k = 0; k < PN; k++) {
                data[j][k] = efovData.getByte(k) != 0;
            }
        }
        return data;

    }

    public boolean readGQisFlagQual(int ifovId) throws IOException {
        final int mdrIndex = computeMdrIndex(ifovId);
        final int efovIndex = computeEfovIndex(ifovId);
        final int ifovIndex = computeIfovIndex(ifovId);

        CompoundData mdrBody = mdrSequence.getCompound(mdrIndex).getCompound(1);
        SequenceData mdrData = mdrBody.getSequence("GQisFlagQual");
        SequenceData efovData = mdrData.getSequence(efovIndex);
        return efovData.getByte(ifovIndex) != 0;
    }

    public byte[] readGEPSIasiMode() throws IOException {
        final byte[] modes = new byte[mdrCount];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            CompoundData mdr = getMdr(mdrIndex);
            modes[mdrIndex] = readMdrGEPSIasiMode(mdr);
        }

        return modes;
    }

    byte readMdrGEPSIasiMode(CompoundData mdr) throws IOException {
        SequenceData bitfield4Bytes = mdr.getSequence("GEPSIasiMode");
        return bitfield4Bytes.getByte(2);
    }
    
    public double[][][] readAllBts(int channelId) throws IOException {
        final double[][][] data = new double[mdrCount][][];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            data[mdrIndex] = readAllBts(channelId, mdrIndex);
        }
        return data;
    }
    
    private double[][] readAllBts(int channelId, int mdrIndex) throws IOException {
        CompoundData mdrBody = mdrSequence.getCompound(mdrIndex).getCompound(1);
        final double iDefSpectDWn1b = EpsFile.readVInt4(mdrBody.getCompound("IDefSpectDWn1b"));
        final int iDefNsFirst1b = mdrBody.getInt("IDefNsfirst1b");
        final double[][] data = new double[SNOT][PN];

        final double[] scaleFactors = giadrScaleFactors.getScaleFactors(iDefNsFirst1b);
        final double wavenumber = iDefSpectDWn1b * (iDefNsFirst1b + channelId - 1);
        
        SequenceData mdrData = mdrBody.getSequence("GS1cSpect");
        for (int j = 0; j < SNOT; j++) {
            SequenceData efovData = mdrData.getSequence(j);
            for (int k = 0; k < PN; k++) {
                SequenceData ifovData = efovData.getSequence(k);
                short spectrumSample = ifovData.getShort(channelId);
                final double radiance = scaleFactors[channelId] * spectrumSample;
                double bt = brightnessTemperature(wavenumber, radiance);
                data[j][k] = bt;
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
        final int mdrIndex = computeMdrIndex(ifovId);
        final int efovIndex = computeEfovIndex(ifovId);
        final int ifovIndex = computeIfovIndex(ifovId);

        CompoundData mdrBody = mdrSequence.getCompound(mdrIndex).getCompound(1);
        final double iDefSpectDWn1b = EpsFile.readVInt4(mdrBody.getCompound("IDefSpectDWn1b"));
        final int iDefNsFirst1b = mdrBody.getInt("IDefNsfirst1b");
        final int iDefNsLast1b = mdrBody.getInt("IDefNslast1b");
        
        final int spectrumSize = iDefNsLast1b - iDefNsFirst1b + 1;
        final double[][] spectrum = new double[spectrumSize][2];

        SequenceData mdrData = mdrBody.getSequence("GS1cSpect");
        SequenceData efovData = mdrData.getSequence(efovIndex);
        SequenceData ifovData = efovData.getSequence(ifovIndex);
        
        final double[] scaleFactors = giadrScaleFactors.getScaleFactors(iDefNsFirst1b);

        for (int i = 0; i < spectrumSize; i++) {
            spectrum[i][0] = (iDefSpectDWn1b * (iDefNsFirst1b + i - 1));
            spectrum[i][1] = (scaleFactors[i] * ifovData.getShort(i));
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
        public final byte[] type = new byte[NCL];
    }

    public RadianceAnalysis readRadianceAnalysis(int ifovId) throws IOException {
        final int mdrIndex = computeMdrIndex(ifovId);
        final int efovIndex = computeEfovIndex(ifovId);
        final int ifovIndex = computeIfovIndex(ifovId);
        final RadianceAnalysis ra = new RadianceAnalysis();
        
        CompoundData mdrBody = mdrSequence.getCompound(mdrIndex).getCompound(1);
        SequenceData channels = mdrBody.getSequence("IDefCcsChannelId");
        for (int i = 0; i < ra.channels.length; i++) {
            ra.channels[i] = channels.getInt(i);
        }
        ra.nbClass = mdrBody.getSequence("GCcsRadAnalNbClass").getSequence(efovIndex).getInt(ifovIndex);
        SequenceData wgt = mdrBody.getSequence("GCcsRadAnalWgt").getSequence(efovIndex).getSequence(ifovIndex);
        for (int i = 0; i < ra.wgt.length; i++) {
            ra.wgt[i] = EpsFile.readVInt4(wgt.getCompound(i));
        }
        SequenceData analY = mdrBody.getSequence("GCcsRadAnalY").getSequence(efovIndex).getSequence(ifovIndex);
        for (int i = 0; i < ra.y.length; i++) {
            ra.y[i] = analY.getInt(i) * 1E-6;
        }
        SequenceData analZ = mdrBody.getSequence("GCcsRadAnalZ").getSequence(efovIndex).getSequence(ifovIndex);
        for (int i = 0; i < ra.z.length; i++) {
            ra.z[i] = analZ.getInt(i) * 1E-6;
        }
        SequenceData mean = mdrBody.getSequence("GCcsRadAnalMean").getSequence(efovIndex).getSequence(ifovIndex);
        for (int i = 0; i < ra.mean.length; i++) {
            SequenceData meanI = mean.getSequence(i);
            for (int j = 0; j < ra.mean[i].length; j++) {
                ra.mean[i][j] = EpsFile.readVInt4(meanI.getCompound(j));
            }
        }
        SequenceData std = mdrBody.getSequence("GCcsRadAnalStd").getSequence(efovIndex).getSequence(ifovIndex);
        for (int i = 0; i < ra.std.length; i++) {
            SequenceData stdI = std.getSequence(i);
            for (int j = 0; j < ra.std[i].length; j++) {
                ra.std[i][j] = EpsFile.readVInt4(stdI.getCompound(j));
            }
        }
        SequenceData image = mdrBody.getSequence("GCcsImageClassified").getSequence(efovIndex);
        for (int i = 0; i < ra.image.length; i++) {
            SequenceData imageI = image.getSequence(i);
            for (int j = 0; j < ra.image[i].length; j++) {
                ra.image[i][j] = imageI.getByte(j);
            }
        }
        ra.mode = mdrBody.getSequence("IDefCcsMode").getByte(3);
        ra.imageH = mdrBody.getSequence("GCcsImageClassifiedNbLin").getShort(efovIndex);
        ra.imageW = mdrBody.getSequence("GCcsImageClassifiedNbCol").getShort(efovIndex);
        ra.avhrrY = EpsFile.readVInt4(mdrBody.getSequence("GCcsImageClassifiedFirstLin").getCompound(efovIndex));
        ra.avhrrX = EpsFile.readVInt4(mdrBody.getSequence("GCcsImageClassifiedFirstCol").getCompound(efovIndex));
//        SequenceData type = mdrBody.getSequence("GCcsRadAnalType");
//        for (int i = 0; i < ra.type.length; i++) {
//            ra.type[i] = type.getByte(i);
//        }
        
        return ra;
    }

    public static class Geometry {
        public double vza;
        public double sza;
        public double vaa;
        public double saa;
    }

    public double[][] readVZA(int mdrIndex) throws IOException {
        CompoundData mdr = getMdr(mdrIndex);
        double[][] vza = new double[SNOT][PN]; 
        SequenceData angles = mdr.getSequence("GGeoSondAnglesMETOP");
        for (int efovIndex = 0; efovIndex < SNOT; efovIndex++) {
            SequenceData efovData = angles.getSequence(efovIndex);
            for (int ifovIndex = 0; ifovIndex < PN; ifovIndex++) {
                SequenceData viewData = efovData.getSequence(ifovIndex);
                vza[efovIndex][ifovIndex] = viewData.getInt(0) * G_GEO_SOND_LOC_SCALING_FACTOR;
            }
        }
        return vza;
    }
    
    public Geometry readGeometry(int ifovId) throws IOException {
        final int mdrIndex = computeMdrIndex(ifovId);
        final int efovIndex = computeEfovIndex(ifovId);
        final int ifovIndex = computeIfovIndex(ifovId);

        Geometry geometry = new Geometry();
        CompoundData mdr = getMdr(mdrIndex);
        SequenceData viewData = mdr.getSequence("GGeoSondAnglesMETOP").getSequence(efovIndex).getSequence(ifovIndex);
        geometry.vza = viewData.getInt(0) * G_GEO_SOND_LOC_SCALING_FACTOR;
        geometry.vaa = viewData.getInt(1) * G_GEO_SOND_LOC_SCALING_FACTOR;
        SequenceData sunData = mdr.getSequence("GGeoSondAnglesSUN").getSequence(efovIndex).getSequence(ifovIndex);
        geometry.sza = sunData.getInt(0) * G_GEO_SOND_LOC_SCALING_FACTOR;
        geometry.saa = sunData.getInt(1) * G_GEO_SOND_LOC_SCALING_FACTOR;
        return geometry;
    }

    public static class NameFilter implements FilenameFilter {

        private final String iasiFilenamePrefix;

        public NameFilter(String avhrrFilename) {
            iasiFilenamePrefix = "IASI" + avhrrFilename.substring(4, 15).replace("1B", "1C");
        }

        public boolean accept(File dir, String name) {
            return name.startsWith(iasiFilenamePrefix) && name.endsWith(".nat");
        }
    }
}
