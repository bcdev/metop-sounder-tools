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

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.SequenceData;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.metop.sounder.SounderFile;
import org.eumetsat.metop.sounder.SounderIfov;
import org.eumetsat.metop.sounder.SounderLayer;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;

abstract class SounderInfoView extends AbstractToolView {
    private SounderFile sounderFile;
    private SounderLayerSelectionListener layerSelectionListener;
    private XYSeriesCollection spectrum;
    private JTextField latTextField;
    private JTextField lonTextField;
    private JTextField mdrIndexTextField;
    private JTextField ifovInMdrIndexTextField;
    private JTextField szaTextField;
    private JTextField saaTextField;
    private JTextField vzaTextField;
    private JTextField vaaTextField;

    @Override
    protected JComponent createControl() {
        final SounderIfov[] ifovs = initLayerModelListenerAndSounderFileFromActivePsvAndReturnSelectedIfovIds();
        if (ifovs.length != 0) {
            update(ifovs);
        }

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Sounder Information", createInfoComponent());
        tabbedPane.add("Sounder Spectrum", createSpectrumChartComponent());

        return tabbedPane;
    }

    protected abstract SounderLayer getActiveSounderLayer();

    protected abstract double[] getSpectrumAbscissaValues();

    protected abstract NumberAxis createSpectrumPlotXAxis();

    protected abstract NumberAxis createSpectrumPlotYAxis();

    protected XYItemRenderer createSpectrumPlotXYItemRenderer() {
        final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        return renderer;
    }

    protected void configureSpectrumPlot(XYPlot spectrumPlot) {
        spectrumPlot.setOrientation(PlotOrientation.VERTICAL);
        spectrumPlot.setNoDataMessage("No data");
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void configureSpectrumChart(JFreeChart spectrumChart) {
    }

    private SounderIfov[] initLayerModelListenerAndSounderFileFromActivePsvAndReturnSelectedIfovIds() {
        VisatApp.getApp().addInternalFrameListener(new ProductSceneViewHook());

        // todo - implement (rq-20090114)
        sounderFile = null;
        layerSelectionListener = new SounderLayerSelectionListenerImpl();
        return new SounderIfov[]{};
    }

    private Component createInfoComponent() {
        latTextField = new JTextField();
        lonTextField = new JTextField();
        mdrIndexTextField = new JTextField();
        ifovInMdrIndexTextField = new JTextField();
        szaTextField = new JTextField();
        saaTextField = new JTextField();
        vzaTextField = new JTextField();
        vaaTextField = new JTextField();

        final TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 0.8);
        layout.setColumnWeightX(2, 1.0);
        layout.setTablePadding(3, 3);

        final JPanel panel = new JPanel(layout);
        panel.add(new JLabel("Latitude:"));
        panel.add(latTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("Longitude:"));
        panel.add(lonTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("MDR index:"));
        panel.add(mdrIndexTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("Ifov index:"));
        panel.add(ifovInMdrIndexTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("Sun zenith:"));
        panel.add(szaTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("Sun azimuth:"));
        panel.add(saaTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("View zenith:"));
        panel.add(vzaTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("View azimuth:"));
        panel.add(vaaTextField);
        panel.add(new JLabel(""));

        final JPanel containerPanel = new JPanel(new BorderLayout(4, 4));
        containerPanel.add(panel, BorderLayout.NORTH);

        return containerPanel;
    }

    private JComponent createSpectrumChartComponent() {
        spectrum = new XYSeriesCollection();

        final JFreeChart chart = createSpectrumChart();
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawHeight(20000);
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMaximumDrawWidth(20000);
        chartPanel.setPreferredSize(new Dimension(200, 200));

        final JPanel containerPanel = new JPanel(new BorderLayout(4, 4));
        containerPanel.add(chartPanel);

        return containerPanel;
    }

    private JFreeChart createSpectrumChart() {
        final NumberAxis xAxis = createSpectrumPlotXAxis();
        final NumberAxis yAxis = createSpectrumPlotYAxis();
        final XYItemRenderer renderer = createSpectrumPlotXYItemRenderer();

        final XYPlot plot = new XYPlot(spectrum, xAxis, yAxis, renderer);
        configureSpectrumPlot(plot);

        final JFreeChart chart = new JFreeChart("Sounder IFOV Spectrum", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        configureSpectrumChart(chart);

        return chart;
    }

    private void update(SounderIfov[] selectedIfovs) {
        if (sounderFile != null) {
            SounderIfov ifov = null;
            if (selectedIfovs.length != 0) {
                ifov = selectedIfovs[0];
            }
            updateInfoFields(ifov);
            updateSpectrumDataset(ifov);
        }
    }

    private void updateInfoFields(SounderIfov ifov) {
        if (ifov == null) {
            clearInfoFields();
            return;
        }

        final GeoPos geoPos;
        final AngularRelation angularRelation;
        try {
            geoPos = readEarthLocation(sounderFile, ifov);
            angularRelation = readAngularRelation(sounderFile, ifov);
        } catch (IOException e) {
            clearInfoFields();
            return;
        }
        lonTextField.setText(geoPos.getLonString());
        latTextField.setText(geoPos.getLatString());

        vzaTextField.setText(Double.toString(angularRelation.vza));
        vaaTextField.setText(Double.toString(angularRelation.vaa));
        szaTextField.setText(Double.toString(angularRelation.sza));
        saaTextField.setText(Double.toString(angularRelation.saa));

        mdrIndexTextField.setText(Integer.toString(ifov.mdrIndex));
        ifovInMdrIndexTextField.setText(Integer.toString(ifov.ifovInMdrIndex));
    }

    private void clearInfoFields() {
        lonTextField.setText("");
        latTextField.setText("");
        saaTextField.setText("");
        vaaTextField.setText("");
        szaTextField.setText("");
        vzaTextField.setText("");
        mdrIndexTextField.setText("");
        ifovInMdrIndexTextField.setText("");
    }

    private void updateSpectrumDataset(SounderIfov ifov) {
        spectrum.removeAllSeries();
        if (ifov == null) {
            return;
        }

        final double[] abscissas = getSpectrumAbscissaValues();
        final double[] radiances;
        try {
            radiances = readSceneRadiances(sounderFile, ifov);
        } catch (IOException e) {
            return;
        }

        final XYSeries series = new XYSeries("Sample Values");
        for (int i = 0; i < radiances.length; i++) {
            series.add(abscissas[i], radiances[i]);
        }

        spectrum.addSeries(series);
    }

    private static AngularRelation readAngularRelation(SounderFile sounderFile, SounderIfov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, "ANGULAR_RELATION", ifov);

        final double sza = numberData.getNumber(0).doubleValue() * 1.0E-2;
        final double vza = numberData.getNumber(1).doubleValue() * 1.0E-2;
        final double saa = numberData.getNumber(2).doubleValue() * 1.0E-2;
        final double vaa = numberData.getNumber(3).doubleValue() * 1.0E-2;

        return new AngularRelation(sza, vza, saa, vaa);
    }

    private static GeoPos readEarthLocation(SounderFile sounderFile, SounderIfov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, "EARTH_LOCATION", ifov);

        final float lat = numberData.getNumber(0).floatValue() * 1.0E-4f;
        final float lon = numberData.getNumber(1).floatValue() * 1.0E-4f;

        return new GeoPos(lat, lon);
    }

    private static double[] readSceneRadiances(SounderFile sounderFile, SounderIfov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, "SCENE_RADIANCE", ifov);

        final double[] radiances = new double[numberData.getElementCount()];
        for (int i = 0; i < radiances.length; i++) {
            radiances[i] = numberData.getNumber(i).doubleValue() * 1.0E-7;
        }

        return radiances;
    }

    private static NumberData getNumberData(SounderFile sounderFile, String sequenceName, SounderIfov ifov) throws IOException {
        return NumberData.create(getSequenceData(sounderFile, sequenceName, ifov));
    }

    private static SequenceData getSequenceData(SounderFile sounderFile, String sequenceName, SounderIfov ifov) throws IOException {
        return getCompoundData(sounderFile, ifov.mdrIndex).getSequence(sequenceName).getSequence(ifov.ifovInMdrIndex);
    }

    private static CompoundData getCompoundData(SounderFile sounderFile, int mdrIndex) throws IOException {
        return sounderFile.getMdrData().getCompound(mdrIndex).getCompound(1);
    }

    private class SounderLayerSelectionListenerImpl implements SounderInfoView.SounderLayerSelectionListener {

        @Override
        public void selectionChanged(SounderLayer layer) {
            // todo - implement (rq-20090114)
            // update(layer.getSelectedIfovs());
        }

    }

    private class ProductSceneViewHook extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final ProductSceneView psv = VisatApp.getApp().getSelectedProductSceneView();
            if (IasiFootprintVPI.isValidAvhrrProductSceneView(psv)) {
                final SounderLayer layer = getActiveSounderLayer();
                if (layer != null) {
                    // todo - implement
//                    modelListener = new IasiListener();
//                    layer.getModel().addListener(modelListener);
//                    iasiFile = layer.getModel().getIasiOverlay().getIasiFile();
                }
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final ProductSceneView psv = VisatApp.getApp().getSelectedProductSceneView();
            if (IasiFootprintVPI.isValidAvhrrProductSceneView(psv)) {
                final SounderLayer layer = getActiveSounderLayer();
                if (layer != null) {
                    // todo - implement
//                    layer.getModel().removeListener(modelListener);
//                    iasiFile = null;
                }
            }
        }

    }

    private interface SounderLayerSelectionListener {
        void selectionChanged(SounderLayer layer);
    }

    private static class AngularRelation {
        // solar-zenith
        public final double vza;
        // sat-zenith
        public final double saa;
        // solar-azimuth
        public final double vaa;
        // sat-azimuth
        public final double sza;

        private AngularRelation(double sza, double vza, double saa, double vaa) {
            this.sza = sza;
            this.vza = vza;
            this.saa = saa;
            this.vaa = vaa;
        }
    }
}
