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
import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceData;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.DefaultImageInfoEditorModel;
import org.esa.beam.framework.ui.ImageInfoEditor;
import org.esa.beam.framework.ui.ImageInfoEditorModel;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.eps.EpsMetaData;
import org.eumetsat.metop.sounder.SounderIfov;
import org.eumetsat.metop.sounder.SounderLayer;
import org.eumetsat.metop.sounder.SounderOverlay;
import org.eumetsat.metop.sounder.SounderOverlayListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

abstract class SounderInfoView extends AbstractToolView {
    private static final String NO_IFOV_SELECTED = "No IFOV selected";

    private SounderOverlayListener overlayListener;
    private InternalFrameListener internalFrameListener;
    private XYSeriesCollection spectrumDataset;
    private JTextField latTextField;
    private JTextField lonTextField;
    private JTextField mdrIndexTextField;
    private JTextField ifovInMdrIndexTextField;
    private JTextField szaTextField;
    private JTextField saaTextField;
    private JTextField vzaTextField;
    private JTextField vaaTextField;
    private ImageInfoEditor editor;

    private class DomainCrosshairValueListener implements ChartProgressListener {
        @Override
        public void chartProgress(ChartProgressEvent event) {
            if (event.getType() != ChartProgressEvent.DRAWING_FINISHED) {
                return;
            }
            final double value = event.getChart().getXYPlot().getDomainCrosshairValue();
            if (value > 0.0) {
                final SounderLayer layer = getSounderLayer();
                if (layer != null) {
                    layer.setSelectedChannel(crosshairValueToSelectedChannel(value));
                    editor.setModel(createImageInfoEditorModel(layer));
                }
            }
        }
    }

    @Override
    protected JComponent createControl() {
        overlayListener = new SounderOverlayListener() {
            @Override
            public void selectionChanged(SounderOverlay overlay) {
                updateUI(overlay);
            }

            @Override
            public void dataChanged(SounderOverlay overlay) {
                updateUI(overlay);
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
        tabbedPane.add("Sounder Layer", createSounderLayerComponent());

        if (IasiFootprintVPI.isValidAvhrrProductSceneViewSelected()) {
            final SounderLayer layer = getSounderLayer();
            if (layer != null) {
                final SounderOverlay overlay = layer.getOverlay();
                overlay.addListener(overlayListener);
                updateUI(overlay);
            }
        }

        return tabbedPane;
    }

    @Override
    public void dispose() {
        VisatApp.getApp().removeInternalFrameListener(internalFrameListener);

        internalFrameListener = null;
        overlayListener = null;
        spectrumDataset = null;

        latTextField = null;
        lonTextField = null;

        mdrIndexTextField = null;
        ifovInMdrIndexTextField = null;

        szaTextField = null;
        saaTextField = null;
        vzaTextField = null;
        vaaTextField = null;
        editor = null;

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

    protected abstract XYSeries createSpectrumPlotXYSeries(double[] radiances);

    protected abstract int crosshairValueToSelectedChannel(double value);

    protected void configureSpectrumChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.white);
        chart.addProgressListener(new DomainCrosshairValueListener());
    }

    protected void configureSpectrumPlot(XYPlot plot) {
        plot.setBackgroundPaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        plot.setDomainCrosshairVisible(true);
        plot.setDomainCrosshairLockedOnData(true);
        plot.setDomainCrosshairValue(1);
        plot.setRangeCrosshairVisible(false);
        plot.setNoDataMessage(NO_IFOV_SELECTED);
    }

    protected void configureSpectrumPlotRenderer(XYLineAndShapeRenderer renderer) {
        renderer.setSeriesShape(0, new Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShapesFilled(0, true);
    }

    protected void configureSpectrumPlotXAxis(NumberAxis axis) {
        axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    }

    protected void configureSpectrumPlotYAxis(NumberAxis axis) {
        axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    }

    protected void configureSpectrumChartPanel(ChartPanel chartPanel) {
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawHeight(20000);
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMaximumDrawWidth(20000);
        chartPanel.setPreferredSize(new Dimension(400, 200));
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

        clearEarthLocationFields();
        clearInfoFields();
        clearAngularRelationFields();

        return containerPanel;
    }

    private JComponent createSpectrumChartComponent() {
        spectrumDataset = new XYSeriesCollection();

        final JFreeChart chart = createSpectrumChart(spectrumDataset);
        configureSpectrumChart(chart);

        final XYPlot plot = chart.getXYPlot();
        configureSpectrumPlot(plot);
        configureSpectrumPlotRenderer((XYLineAndShapeRenderer) plot.getRenderer());
        configureSpectrumPlotYAxis((NumberAxis) plot.getRangeAxis());
        configureSpectrumPlotXAxis((NumberAxis) plot.getDomainAxis());

        final ChartPanel chartPanel = new ChartPanel(chart);
        configureSpectrumChartPanel(chartPanel);

        final JPanel containerPanel = new JPanel(new BorderLayout(4, 4));
        containerPanel.add(chartPanel);

        return containerPanel;
    }

    private Component createSounderLayerComponent() {
        editor = new ImageInfoEditor();

        final JPanel containerPanel = new JPanel(new BorderLayout(4, 4));
        containerPanel.add(editor, BorderLayout.NORTH);

        final SounderLayer layer = getSounderLayer();
        if (layer != null) {
            editor.setModel(createImageInfoEditorModel(layer));
        }

        return containerPanel;
    }

    private ImageInfoEditorModel createImageInfoEditorModel(final SounderLayer layer) {
        final ImageInfoEditorModel editorModel = new DefaultImageInfoEditorModel(layer.getImageInfo());

        editorModel.setDisplayProperties("Name", "unit", layer.getStx(), layer.getScaling());
        editorModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                layer.regenerate();
            }
        });
        return editorModel;
    }

    private void updateUI(final SounderOverlay overlay) {
        assert overlay != null;
        final SounderIfov selectedIfov = overlay.getSelectedIfov();
        if (selectedIfov != null) {
            updateEarthLocationFields(selectedIfov, overlay.getEpsFile());
            updateInfoFields(selectedIfov);
            updateAngularRelationFields(selectedIfov, overlay.getEpsFile());
            updateSpectrumDataset(overlay.getSelectedIfov(), overlay.getEpsFile());
        } else {
            clearInfoFields();
            clearEarthLocationFields();
            clearAngularRelationFields();
        }
    }

    private void updateInfoFields(SounderIfov selectedIfov) {
        mdrIndexTextField.setText(Integer.toString(selectedIfov.mdrIndex));
        ifovInMdrIndexTextField.setText(Integer.toString(selectedIfov.ifovInMdrIndex));
    }

    private void updateEarthLocationFields(final SounderIfov selectedIfov, final EpsFile epsFile) {
        final SwingWorker<GeoPos, Object> worker = new SwingWorker<GeoPos, Object>() {
            @Override
            protected GeoPos doInBackground() throws Exception {
                return readEarthLocation(epsFile, selectedIfov);
            }

            @Override
            protected void done() {
                try {
                    final GeoPos geoPos = get();
                    lonTextField.setText(geoPos.getLonString());
                    latTextField.setText(geoPos.getLatString());
                } catch (InterruptedException e) {
                    clearEarthLocationFields();
                } catch (ExecutionException e) {
                    clearEarthLocationFields();
                }
            }
        };
        worker.execute();
    }

    private void updateAngularRelationFields(final SounderIfov selectedIfov, final EpsFile epsFile) {
        final SwingWorker<AngularRelation, Object> worker = new SwingWorker<AngularRelation, Object>() {
            @Override
            protected AngularRelation doInBackground() throws Exception {
                return readAngularRelation(epsFile, selectedIfov);
            }

            @Override
            protected void done() {
                try {
                    final AngularRelation angularRelation = get();
                    vzaTextField.setText(Double.toString(angularRelation.vza));
                    vaaTextField.setText(Double.toString(angularRelation.vaa));
                    szaTextField.setText(Double.toString(angularRelation.sza));
                    saaTextField.setText(Double.toString(angularRelation.saa));
                } catch (InterruptedException e) {
                    clearAngularRelationFields();
                } catch (ExecutionException e) {
                    clearAngularRelationFields();
                }
            }
        };
        worker.execute();
    }

    private void clearInfoFields() {
        mdrIndexTextField.setText(NO_IFOV_SELECTED);
        ifovInMdrIndexTextField.setText(NO_IFOV_SELECTED);
    }

    private void clearEarthLocationFields() {
        lonTextField.setText(NO_IFOV_SELECTED);
        latTextField.setText(NO_IFOV_SELECTED);
    }

    private void clearAngularRelationFields() {
        saaTextField.setText(NO_IFOV_SELECTED);
        vaaTextField.setText(NO_IFOV_SELECTED);
        szaTextField.setText(NO_IFOV_SELECTED);
        vzaTextField.setText(NO_IFOV_SELECTED);
    }

    private void updateSpectrumDataset(final SounderIfov selectedIfov, final EpsFile epsFile) {
        final SwingWorker<XYSeries, Object> worker = new SwingWorker<XYSeries, Object>() {
            @Override
            protected XYSeries doInBackground() throws Exception {
                return createSpectrumPlotXYSeries(readSceneRadiances(epsFile, selectedIfov));
            }

            @Override
            protected void done() {
                try {
                    spectrumDataset.removeAllSeries();
                    final XYSeries series = get();
                    spectrumDataset.addSeries(series);
                } catch (InterruptedException e) {
                    // ignore
                } catch (ExecutionException e) {
                    // ignore
                }
            }
        };
        worker.execute();
    }

    private AngularRelation readAngularRelation(EpsFile sounderFile, SounderIfov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, getAngularRelationSequenceName(), ifov);

        final double factor = getScalingFactor(sounderFile, getEarthLocationSequenceName()).doubleValue();
        final double sza = numberData.getNumber(0).doubleValue() / factor;
        final double vza = numberData.getNumber(1).doubleValue() / factor;
        final double saa = numberData.getNumber(2).doubleValue() / factor;
        final double vaa = numberData.getNumber(3).doubleValue() / factor;

        return new AngularRelation(sza, vza, saa, vaa);
    }

    private GeoPos readEarthLocation(EpsFile sounderFile, SounderIfov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, getEarthLocationSequenceName(), ifov);

        final float factor = getScalingFactor(sounderFile, getEarthLocationSequenceName()).floatValue();
        final float lat = numberData.getNumber(0).floatValue() / factor;
        final float lon = numberData.getNumber(1).floatValue() / factor;

        return new GeoPos(lat, lon);
    }

    private double[] readSceneRadiances(EpsFile sounderFile, SounderIfov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, getSceneRadianceSequenceName(), ifov);

        final double factor = getScalingFactor(sounderFile, getSceneRadianceSequenceName()).doubleValue();
        final double[] radiances = new double[numberData.getElementCount()];
        for (int i = 0; i < radiances.length; i++) {
            radiances[i] = numberData.getNumber(i).doubleValue() / factor;
        }

        return radiances;
    }

    private static JFreeChart createSpectrumChart(XYSeriesCollection dataset) {
        return ChartFactory.createXYLineChart(
                "Sounder IFOV Spectrum",         // chart title
                "Channel",                       // x axis label
                "Brightness Temperature (K)",    // y axis label
                dataset,
                PlotOrientation.VERTICAL,
                false,                           // include legend
                true,                            // tooltips
                false                            // urls
        );
    }

    private static EpsMetaData getMetaData(EpsFile sounderFile, String sequenceName) throws IOException {
        final CompoundData compoundData = getCompoundData(sounderFile, 0);
        final CompoundType compoundType = compoundData.getCompoundType();
        final CompoundMember compoundMember = compoundType.getMember(compoundType.getMemberIndex(sequenceName));

        return (EpsMetaData) compoundMember.getMetadata();
    }

    private static Number getScalingFactor(EpsFile sounderFile, String sequenceName) throws IOException {
        try {
            return Double.valueOf(getMetaData(sounderFile, sequenceName).getScalingFactor().replace("10^", "1.0E"));
        } catch (NumberFormatException e) {
            throw new IOException(e.getMessage(), e);
        }
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
