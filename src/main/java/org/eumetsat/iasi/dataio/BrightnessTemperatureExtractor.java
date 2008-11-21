package org.eumetsat.iasi.dataio;

import static org.eumetsat.iasi.dataio.AuxiliaryDataFormats.*;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.bc.ceres.binio.Format;
import com.bc.ceres.binio.IOContext;
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.util.ImageIOHandler;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class BrightnessTemperatureExtractor {

    private static final String[] RESOURCE_PATHS = {
            "/auxdata/IASI_L2_PGS_THR_CLDDET",
            "/auxdata/IASI_L2_PGS_COF_IASINT",
            "/auxdata/IASI_L2_PGS_THR_HORCO",
            "/auxdata/IASI_L2_PGS_THR_EOFRES",
            "/auxdata/IASI_L2_PGS_THR_WINCOR",
            "/auxdata/IASI_L2_PGS_THR_POLCLD",
            "/auxdata/IASI_L2_PGS_THR_DESSTR"};

    private static final Format[] FORMATS = {
            IASI_L2_PGS_THR_CLDDET_FORMAT,
            IASI_L2_PGS_COF_IASINT_FORMAT,
            IASI_L2_PGS_THR_HORCO_FORMAT,
            IASI_L2_PGS_THR_EOFRES_FORMAT,
            IASI_L2_PGS_THR_WINCOR_FORMAT,
            IASI_L2_PGS_THR_POLCLD_FORMAT,
            IASI_L2_PGS_THR_DESSTR_FORMAT};

    private final int[][] channels;
    private RadianceTuner radianceTuner;

    private BrightnessTemperatureExtractor() {
        channels = new int[RESOURCE_PATHS.length][];
    }

    public int[] getBtValueCounts() {
        final int[] counts = new int[channels.length];

        for (int i = 0; i < counts.length; ++i) {
            if (i != 2) {
                counts[i] = channels[i].length;
            } else { // horizontal coherence test
                counts[i] = 4 * channels[i].length;
            }
        }

        return counts;
    }

    public static BrightnessTemperatureExtractor create() throws IOException {
        final BrightnessTemperatureExtractor extractor = new BrightnessTemperatureExtractor();

        for (int k = 0; k < RESOURCE_PATHS.length; k++) {
            final InputStream is = AuxiliaryDataFormats.class.getResourceAsStream(RESOURCE_PATHS[k]);
            final MemoryCacheImageInputStream iis = new MemoryCacheImageInputStream(is);

            try {
                final IOContext context = new IOContext(FORMATS[k], new ImageIOHandler(iis));
                final CompoundData data = context.getData();

                final SequenceData channelData = data.getSequence(CHANNEL_IDENTIFICATION_NUMBERS);
                extractor.channels[k] = new int[channelData.getElementCount()];

                for (int i = 0; i < channelData.getElementCount(); ++i) {
                    extractor.channels[k][i] = channelData.getInt(i) - 1;
                }
            } finally {
                iis.close();
            }
        }

        return extractor;
    }

    public double[][] extractBrightnessTemperatures(double[][][] efovSpecra, int ifovInEfovIndex) throws IOException {
        synchronized (this) {
            if (radianceTuner == null) {
                radianceTuner = RadianceTuner.create();
            }
        }

        final double[][] bt = new double[efovSpecra.length][];

        for (int i = 0; i < efovSpecra.length; ++i) {
            final double[][] ifovSpectrum = efovSpecra[i];
            bt[i] = new double[ifovSpectrum.length];

            for (int k = 0; k < ifovSpectrum.length; ++k) {
                final double[] sample = ifovSpectrum[k];
                bt[i][k] = brightnessTemperature(sample[0], sample[1]);
            }

            bt[i] = radianceTuner.tune(bt[i]);
        }

        final double[][] extractedBt = new double[channels.length][];

        for (int i = 0; i < channels.length; i++) {
            final int channelCount = channels[i].length;

            if (i != 2) {
                extractedBt[i] = new double[channelCount];
                for (int k = 0; k < channelCount; k++) {
                    extractedBt[i][k] = bt[ifovInEfovIndex][channels[i][k]];
                }
            } else { // horizontal coherence test
                extractedBt[i] = new double[4 * channelCount];
                for (int j = 0; j < 4; ++j) {
                    for (int k = 0; k < channelCount; k++) {
                        extractedBt[i][j * channelCount + k] = bt[j][channels[i][k]];
                    }
                }
            }
        }

        return extractedBt;
    }

    /**
     * Calculates the brightness temperature corresponding to a radiance measurement
     * by means of Planck's law.
     *
     * @param wavenumber the wavenumber (cm-1)
     * @param radiance   the radiance (W/m2/sr/m-1)
     * @return the brightness temperature (K)
     */
    private static double brightnessTemperature(double wavenumber, double radiance) {
        final double c1 = 1.191042722E-16;
        final double c2 = 1.4387752E-2;
        final double a = c2 * wavenumber;
        final double b = c1 * wavenumber * wavenumber * wavenumber;

        double d = a / (Math.log(1.0 + (b / radiance)));
        if (Double.isNaN(d)) {
            d = 0.0;
        }
        return d;
    }
}
