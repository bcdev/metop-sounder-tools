/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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

import org.eumetsat.metop.mhs.MhsSounderLayer;
import org.eumetsat.metop.sounder.SounderLayer;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.JFreeChart;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;

/**
 * Tool view for showing information on the selected MHS field-of-view.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class MhsInfoView extends SounderInfoView {
    private static final double[] CHANNEL_FREQUENCIES = new double[5];

    static {
        CHANNEL_FREQUENCIES[0] = 89.0;
        CHANNEL_FREQUENCIES[1] = 157.0;
        CHANNEL_FREQUENCIES[2] = 183.311;
        CHANNEL_FREQUENCIES[3] = 183.311;
        CHANNEL_FREQUENCIES[4] = 190.311;
    }

    @Override
    protected String getSceneRadianceSequenceName() {
        return "SCENE_RADIANCES";
    }

    @Override
    protected SounderLayer getSounderLayer() {
        return IasiFootprintVPI.getActiveFootprintLayer(MhsSounderLayer.class);
    }

    @Override
    protected NumberAxis createSpectrumPlotXAxis() {
        final NumberAxis axis = new NumberAxis("Channel");
        axis.setRange(0.0, CHANNEL_FREQUENCIES.length + 1);

        return axis;
    }

    @Override
    protected NumberAxis createSpectrumPlotYAxis() {
        final NumberAxis axis = new NumberAxis("Brightness Temperature (K)");
        axis.setRange(new Range(175.0, 325.0));

        return axis;
    }

    @Override
    protected XYSeries createSpectrumPlotXYSeries(double[] radiances) {
        final XYSeries series = new XYSeries("Sample Values");

        for (int i = 0; i < CHANNEL_FREQUENCIES.length; i++) {
            series.add(i + 1, BlackBody.temperatureAtFrequency(CHANNEL_FREQUENCIES[i], radiances[i] * 0.1));
        }

        return series;
    }

    @Override
    protected void configureSpectrumChart(JFreeChart spectrumChart) {
        spectrumChart.setTitle("MHS IFOV Spectrum");
    }
}
