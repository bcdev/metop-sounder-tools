/*
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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

package org.eumetsat.iasi.dataio;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: $ $Date: $
 */
public class RadianceTuner {

    private static final int CHANNEL_COUNT = 8481;

    private final long[] coefficientCounts;
    private final long[][] channelIdentifiers;
    private final double[][] coefficients;

    private RadianceTuner() {
        coefficientCounts = new long[CHANNEL_COUNT];
        channelIdentifiers = new long[CHANNEL_COUNT][100];
        coefficients = new double[CHANNEL_COUNT][101];
    }

    public static RadianceTuner create() throws IOException {
        final InputStream is = RadianceTuner.class.getResourceAsStream("/auxdata/IASI_L2_PGS_COF_TUNRAD");
        final ImageInputStream iis = new MemoryCacheImageInputStream(is);
        iis.setByteOrder(ByteOrder.BIG_ENDIAN);

        final RadianceTuner tuner = new RadianceTuner();

        iis.readFully(tuner.coefficientCounts, 0, tuner.coefficientCounts.length);
        for (final long[] values : tuner.channelIdentifiers) {
            iis.readFully(values, 0, 100);
        }
        for (final double[] values : tuner.coefficients) {
            iis.readFully(values, 0, 101);
        }

        return tuner;
    }

    public double[] tune(double[] bt) {
        double[] tunedBt = new double[bt.length];

        for (int i = 0; i < bt.length; i++) {
            double sum = 0.0;

            for (int j = 0; j < coefficientCounts[i]; j++) {
                sum += coefficients[i][j + 1] * bt[(int) channelIdentifiers[i][j] - 1];
            }

            tunedBt[i] = coefficients[i][0] + sum;
        }

        return tunedBt;
    }
}
