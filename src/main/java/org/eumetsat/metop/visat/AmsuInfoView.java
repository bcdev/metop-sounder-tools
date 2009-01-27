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

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.mhs.MhsSounderLayer;
import org.eumetsat.metop.sounder.Ifov;
import org.eumetsat.metop.sounder.SounderLayer;
import org.eumetsat.metop.amsu.AmsuSounderLayer;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;

import java.io.IOException;


/**
 * Tool view for showing information on the selected AMSU field-of-view.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class AmsuInfoView extends SounderInfoView {
    private static final double[] CHANNEL_FREQUENCIES = new double[15];

    static {
        CHANNEL_FREQUENCIES[0] = 23.80;
        CHANNEL_FREQUENCIES[1] = 31.40;
        CHANNEL_FREQUENCIES[2] = 50.30;
        CHANNEL_FREQUENCIES[3] = 52.80;
        CHANNEL_FREQUENCIES[4] = 53.59;
        CHANNEL_FREQUENCIES[5] = 54.40;
        CHANNEL_FREQUENCIES[6] = 54.94;
        CHANNEL_FREQUENCIES[7] = 55.50;
        CHANNEL_FREQUENCIES[8] = 57.290344;
        CHANNEL_FREQUENCIES[9] = 57.290344;
        CHANNEL_FREQUENCIES[10] = 57.290344;
        CHANNEL_FREQUENCIES[11] = 57.290344;
        CHANNEL_FREQUENCIES[12] = 57.290344;
        CHANNEL_FREQUENCIES[13] = 57.290344;
        CHANNEL_FREQUENCIES[14] = 89.0;
    }

    @Override
    protected SounderLayer getSounderLayer(ProductSceneView view) {
        return getLayer(view.getRootLayer(), AmsuSounderLayer.class);
    }

    private static <T extends Layer> T getLayer(Layer parentLayer, Class<T> layerClass) {
        if (layerClass.isAssignableFrom(parentLayer.getClass())) {
            return (T) parentLayer;
        }
        for (final Layer childLayer : parentLayer.getChildren()) {
            final T layer = getLayer(childLayer, layerClass);
            if (layer != null) {
                return layer;
            }
        }
        return null;
    }

    @Override
    protected XYSeries createSpectrumPlotXYSeries(final double[] radiances) {
        final XYSeries series = new XYSeries("Sample Values");

        for (int i = 0; i < CHANNEL_FREQUENCIES.length; i++) {
            series.add(i + 1, BlackBody.temperatureAtFrequency(CHANNEL_FREQUENCIES[i], radiances[i] * 0.1));
        }

        return series;
    }

    @Override
    protected int crosshairValueToChannel(double value) {
        return (int) (value - 1.0);
    }

    @Override
    protected double channelToCrosshairValue(int channel) {
        return channel + 1.0;
    }

    @Override
    protected void configureSpectrumPlotXAxis(NumberAxis axis) {
        super.configureSpectrumPlotXAxis(axis);
        axis.setRange(new Range(0.5, CHANNEL_FREQUENCIES.length + 0.5), true, false);
    }

    @Override
    protected void configureSpectrumPlotYAxis(NumberAxis axis) {
        super.configureSpectrumPlotYAxis(axis);
        axis.setRange(new Range(0.0, 500.0), true, false);
    }

    @Override
    protected void configureSpectrumChart(final JFreeChart chart) {
        super.configureSpectrumChart(chart);
        chart.setTitle("AMSU IFOV Spectrum");
    }

    @Override
    protected final GeoPos readEarthLocation(EpsFile sounderFile, Ifov ifov) throws IOException {
        return readEarthLocation(sounderFile, "EARTH_LOCATION", ifov);
    }

    @Override
    protected final AngularRelation readAngularRelation(EpsFile sounderFile, Ifov ifov) throws IOException {
        return readAngularRelation(sounderFile, "ANGULAR_RELATION", ifov);
    }

    @Override
    protected final double[] readSceneRadiances(EpsFile sounderFile, Ifov ifov) throws IOException {
        return readSceneRadiances(sounderFile, "SCENE_RADIANCE", ifov);
    }
}
