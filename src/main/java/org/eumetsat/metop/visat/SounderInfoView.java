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
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.CompoundMember;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.eps.EpsMetaData;
import org.eumetsat.metop.sounder.*;
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
import javax.swing.event.InternalFrameListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;

abstract class SounderInfoView extends AbstractToolView {
    private SounderOverlayListener overlayListener;
    private InternalFrameListener internalFrameListener;
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
        overlayListener = new SounderOverlayListener() {
            @Override
            public void selectionChanged(SounderOverlay overlay) {
                update(overlay);
            }
        };
        internalFrameListener = new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                if (IasiFootprintVPI.isValidAvhrrProductSceneViewSelected()) {
                    final SounderLayer layer = getSounderLayer();
                    if (layer != null) {
                        layer.getOverlay().addListener(overlayListener);
                    }
                }
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent e) {
                if (IasiFootprintVPI.isValidAvhrrProductSceneViewSelected()) {
                    final SounderLayer layer = getSounderLayer();
                    if (layer != null) {
                        layer.getOverlay().removeListener(overlayListener);
                    }
                }
            }
        };
        VisatApp.getApp().addInternalFrameListener(internalFrameListener);

        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Sounder Info", createInfoComponent());
        tabbedPane.add("Sounder Spectrum", createSpectrumChartComponent());

        if (IasiFootprintVPI.isValidAvhrrProductSceneViewSelected()) {
            final SounderLayer layer = getSounderLayer();
            if (layer != null) {
                final SounderOverlay overlay = layer.getOverlay();
                overlay.addListener(overlayListener);
                update(overlay);
            }
        }


        return tabbedPane;
    }

    @Override
    public void dispose() {
        VisatApp.getApp().removeInternalFrameListener(internalFrameListener);
        super.dispose();
    }

    protected String getSceneRadianceSequenceName() {
        return "SCENE_RADIANCE";
    }
    
    protected String getEarthLocationSequenceName() {
        return "EARTH_LOCATION";
    }

    protected String getAngularRelationSequenceName() {
        return "ANGULAR_RELATION";
    }

    protected abstract SounderLayer getSounderLayer();

    protected abstract NumberAxis createSpectrumPlotXAxis();

    protected abstract NumberAxis createSpectrumPlotYAxis();

    protected abstract XYSeries createSpectrumPlotXYSeries(double[] radiances);

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
        chartPanel.setPreferredSize(new Dimension(400, 200));

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

    private void update(SounderOverlay overlay) {
        if (overlay != null) {
            updateInfoFields(overlay);
            updateSpectrumDataset(overlay);
        }
    }

    private void updateInfoFields(SounderOverlay overlay) {
        final SounderIfov selectedIfov = overlay.getSelectedIfov();
        if (selectedIfov == null) {
            clearInfoFields();
            return;
        }

        final GeoPos geoPos;
        final AngularRelation angularRelation;
        try {
            geoPos = readEarthLocation(overlay.getEpsFile(), selectedIfov);
            angularRelation = readAngularRelation(overlay.getEpsFile(), selectedIfov);
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

        mdrIndexTextField.setText(Integer.toString(selectedIfov.mdrIndex));
        ifovInMdrIndexTextField.setText(Integer.toString(selectedIfov.ifovInMdrIndex));
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

    private void updateSpectrumDataset(SounderOverlay overlay) {
        spectrum.removeAllSeries();
        final SounderIfov selectedIfov = overlay.getSelectedIfov();
        if (selectedIfov == null) {
            return;
        }

        final double[] radiances;
        try {
            radiances = readSceneRadiances(overlay.getEpsFile(), selectedIfov);
        } catch (IOException e) {
            return;
        }

        final XYSeries series = createSpectrumPlotXYSeries(radiances);

        spectrum.addSeries(series);
    }

    private AngularRelation readAngularRelation(EpsFile sounderFile, SounderIfov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, getAngularRelationSequenceName(), ifov);

        final double factor = SounderConstants.ANGULAR_RELATION_SCALING_FACTOR;
        final double sza = numberData.getNumber(0).doubleValue() * factor;
        final double vza = numberData.getNumber(1).doubleValue() * factor;
        final double saa = numberData.getNumber(2).doubleValue() * factor;
        final double vaa = numberData.getNumber(3).doubleValue() * factor;

        return new AngularRelation(sza, vza, saa, vaa);
    }

    private GeoPos readEarthLocation(EpsFile sounderFile, SounderIfov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, getEarthLocationSequenceName(), ifov);

        final float factor = (float) SounderConstants.EARTH_LOCATION_SCALING_FACTOR;
        final float lat = numberData.getNumber(0).floatValue() * factor;
        final float lon = numberData.getNumber(1).floatValue() * factor;

        return new GeoPos(lat, lon);
    }

    private double[] readSceneRadiances(EpsFile sounderFile, SounderIfov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, getSceneRadianceSequenceName(), ifov);

        final double factor = SounderConstants.SCENE_RADIANCE_SCALING_FACTOR;
        final double[] radiances = new double[numberData.getElementCount()];
        for (int i = 0; i < radiances.length; i++) {
            radiances[i] = numberData.getNumber(i).doubleValue() * factor;
        }

        return radiances;
    }

    private static EpsMetaData getMetaData(EpsFile sounderFile, String sequenceName) throws IOException {
        final CompoundData compoundData = getCompoundData(sounderFile, 0);
        final CompoundType compoundType = compoundData.getCompoundType();
        final CompoundMember compoundMember = compoundType.getMember(compoundType.getMemberIndex(sequenceName));

        return (EpsMetaData) compoundMember.getMetadata();
    }

    private static NumberData getNumberData(EpsFile sounderFile, String sequenceName, SounderIfov ifov) throws IOException {
        return NumberData.of(getSequenceData(sounderFile, sequenceName, ifov));
    }

    private static SequenceData getSequenceData(EpsFile sounderFile, String sequenceName, SounderIfov ifov) throws IOException {
        return getCompoundData(sounderFile, ifov.mdrIndex).getSequence(sequenceName).getSequence(ifov.ifovInMdrIndex);
    }

    private static CompoundData getCompoundData(EpsFile sounderFile, int mdrIndex) throws IOException {
        return sounderFile.getMdrData().getCompound(mdrIndex).getCompound(1);
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
