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
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.DefaultImageInfoEditorModel;
import org.esa.beam.framework.ui.ImageInfoEditor;
import org.esa.beam.framework.ui.ImageInfoEditorModel;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.eps.EpsMetaData;
import org.eumetsat.metop.sounder.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
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
    private static final String ACCESS_ERROR = "Data access error";

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
    private XYPlot spectrumPlot;
    
    private SounderOverlay currentOverlay;

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
                final SounderLayer layer = getSounderLayer();
                if (layer != null) {
                    modelChanged(layer);
                } else {
                    final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
                    final LayerListener layerListener = new AbstractLayerListener() {
                        @Override
                        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
                            final SounderLayer layer = getSounderLayer();
                            if (layer != null) {
                                modelChanged(layer);
                                view.getRootLayer().removeListener(this);
                            }
                        }
                    };
                    view.getRootLayer().addListener(layerListener);
                }
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent e) {
                if (currentOverlay != null) {
                    currentOverlay.removeListener(overlayListener);
                }
                updateUI(null);
                editor.setModel(null);
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
                modelChanged(layer);
            }
        }

        return tabbedPane;
    }
    
    private void modelChanged(SounderLayer layer) {
        currentOverlay = layer.getOverlay();
        currentOverlay.addListener(overlayListener);

        final int channel = layer.getSelectedChannel();
        final double crosshairValue = channelToCrosshairValue(channel);
        spectrumPlot.setDomainCrosshairValue(crosshairValue);
        editor.setModel(createImageInfoEditorModel(layer));
        updateUI(currentOverlay);
    }
    
    @Override
    public void componentFocusGained() {
        ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (IasiFootprintVPI.isValidAvhrrProductSceneView(productSceneView)) {
            SounderLayer layer = getSounderLayer();
            if (layer != null) {
                productSceneView.setSelectedLayer(layer);
            }
        }
    }

    @Override
    public void dispose() {
        VisatApp.getApp().removeInternalFrameListener(internalFrameListener);

        internalFrameListener = null;
        overlayListener = null;
        spectrumDataset = null;
        spectrumPlot = null;

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

    protected abstract SounderLayer getSounderLayer();

    protected abstract XYSeries createSpectrumPlotXYSeries(double[] radiances);

    protected abstract int crosshairValueToChannel(double value);

    protected abstract double channelToCrosshairValue(int channel);

    protected void configureSpectrumChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.white);
    }

    protected void configureSpectrumPlot(XYPlot plot) {
        plot.setBackgroundPaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        plot.setDomainCrosshairVisible(true);
        plot.setDomainCrosshairLockedOnData(true);
        plot.setRangeCrosshairVisible(false);
        plot.setNoDataMessage(NO_IFOV_SELECTED);
    }

    protected void configureSpectrumPlotRenderer(XYLineAndShapeRenderer renderer) {
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

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

        clearEarthLocationFields(NO_IFOV_SELECTED);
        clearInfoFields(NO_IFOV_SELECTED);
        clearAngularRelationFields(NO_IFOV_SELECTED);

        return containerPanel;
    }

    private JComponent createSpectrumChartComponent() {
        spectrumDataset = new XYSeriesCollection();

        final JFreeChart chart = ChartFactory.createXYLineChart(
                "Sounder IFOV Spectrum",         // chart title
                "Channel",                       // x axis label
                "Brightness Temperature (K)",    // y axis label
                spectrumDataset,
                PlotOrientation.VERTICAL,
                false,                           // include legend
                true,                            // tooltips
                false                            // urls
        );
        chart.addProgressListener(new DomainCrosshairListener());
        configureSpectrumChart(chart);

        spectrumPlot = chart.getXYPlot();
        configureSpectrumPlot(spectrumPlot);
        configureSpectrumPlotRenderer((XYLineAndShapeRenderer) spectrumPlot.getRenderer());
        configureSpectrumPlotYAxis((NumberAxis) spectrumPlot.getRangeAxis());
        configureSpectrumPlotXAxis((NumberAxis) spectrumPlot.getDomainAxis());

        final ChartPanel chartPanel = new ChartPanel(chart);
        configureSpectrumChartPanel(chartPanel);

        final JPanel containerPanel = new JPanel(new BorderLayout(4, 4));
        containerPanel.add(chartPanel);

        return containerPanel;
    }

    private class DomainCrosshairListener implements ChartProgressListener {
        @Override
        public void chartProgress(ChartProgressEvent event) {
            if (event.getType() != ChartProgressEvent.DRAWING_FINISHED) {
                return;
            }
            final double value = event.getChart().getXYPlot().getDomainCrosshairValue();
            if (value > 0.0) {
                final SounderLayer layer = getSounderLayer();
                if (layer != null) {
                    final int channel = crosshairValueToChannel(value);
                    if (channel != layer.getSelectedChannel()) {
                        final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                            @Override
                            protected Object doInBackground() throws Exception {
                                layer.setSelectedChannel(channel);
                                return null;
                            }

                            @Override
                            protected void done() {
                                editor.setModel(createImageInfoEditorModel(layer));
                            }
                        };
                        worker.execute();
                    } else {
                        if (editor.getModel() == null) {
                            editor.setModel(createImageInfoEditorModel(layer));
                        }
                    }
                }
            }
        }
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
        final ImageInfo imageInfo = layer.getImageInfo();
        final ImageInfoEditorModel editorModel = new DefaultImageInfoEditorModel(imageInfo);

        final Stx stx = layer.getStx();
        final Scaling scaling = layer.getScaling();

        editorModel.setDisplayProperties("", "", stx, scaling);
        editorModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                layer.regenerate();
            }
        });

        return editorModel;
    }

    protected void updateUI(final SounderOverlay overlay) {
        if (overlay != null && overlay.getSelectedIfov() != null) {
            final Ifov selectedIfov = overlay.getSelectedIfov();
            updateEarthLocationFields(selectedIfov, overlay.getEpsFile());
            updateIndexFields(selectedIfov);
            updateAngularRelationFields(selectedIfov, overlay.getEpsFile());
            updateSpectrumDataset(overlay.getSelectedIfov(), overlay.getEpsFile());
        } else {
            clearInfoFields(NO_IFOV_SELECTED);
            clearEarthLocationFields(NO_IFOV_SELECTED);
            clearAngularRelationFields(NO_IFOV_SELECTED);
            spectrumDataset.removeAllSeries();
            spectrumPlot.setNoDataMessage(NO_IFOV_SELECTED);
        }
    }

    private void updateIndexFields(Ifov selectedIfov) {
        mdrIndexTextField.setText(Integer.toString(selectedIfov.getMdrIndex()));
        ifovInMdrIndexTextField.setText(Integer.toString(selectedIfov.getIfovInMdrIndex()));
    }

    private void updateEarthLocationFields(final Ifov selectedIfov, final EpsFile epsFile) {
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
                    clearEarthLocationFields(ACCESS_ERROR);
                } catch (ExecutionException e) {
                    clearEarthLocationFields(ACCESS_ERROR);
                }
            }
        };
        worker.execute();
    }

    private void updateAngularRelationFields(final Ifov selectedIfov, final EpsFile epsFile) {
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
                    clearAngularRelationFields(ACCESS_ERROR);
                } catch (ExecutionException e) {
                    clearAngularRelationFields(ACCESS_ERROR);
                }
            }
        };
        worker.execute();
    }

    private void clearInfoFields(String text) {
        mdrIndexTextField.setText(text);
        ifovInMdrIndexTextField.setText(text);
    }

    private void clearEarthLocationFields(String text) {
        lonTextField.setText(text);
        latTextField.setText(text);
    }

    private void clearAngularRelationFields(String text) {
        saaTextField.setText(text);
        vaaTextField.setText(text);
        szaTextField.setText(text);
        vzaTextField.setText(text);
    }

    private void updateSpectrumDataset(final Ifov selectedIfov, final EpsFile epsFile) {
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
                    spectrumPlot.setNoDataMessage(ACCESS_ERROR);
                } catch (ExecutionException e) {
                    spectrumPlot.setNoDataMessage(ACCESS_ERROR);
                }
            }
        };
        worker.execute();
    }

    protected abstract GeoPos readEarthLocation(EpsFile sounderFile, Ifov ifov) throws IOException;

    protected abstract AngularRelation readAngularRelation(EpsFile sounderFile, Ifov ifov) throws IOException;

    protected abstract double[] readSceneRadiances(EpsFile sounderFile, Ifov ifov) throws IOException;

    protected static GeoPos readEarthLocation(EpsFile sounderFile, String sequenceName, Ifov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, sequenceName, ifov);

        final float factor = getScalingFactor(sounderFile, sequenceName).floatValue();
        final float lat = numberData.getNumber(0).floatValue() / factor;
        final float lon = numberData.getNumber(1).floatValue() / factor;

        return new GeoPos(lat, lon);
    }

    protected static AngularRelation readAngularRelation(EpsFile sounderFile, String sequenceName, Ifov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, sequenceName, ifov);

        final double factor = getScalingFactor(sounderFile, sequenceName).doubleValue();
        final double sza = numberData.getNumber(0).doubleValue() / factor;
        final double vza = numberData.getNumber(1).doubleValue() / factor;
        final double saa = numberData.getNumber(2).doubleValue() / factor;
        final double vaa = numberData.getNumber(3).doubleValue() / factor;

        return new AngularRelation(sza, vza, saa, vaa);
    }

    protected static double[] readSceneRadiances(EpsFile sounderFile, String sequenceName, Ifov ifov) throws IOException {
        final NumberData numberData = getNumberData(sounderFile, sequenceName, ifov);

        final double factor = getScalingFactor(sounderFile, sequenceName).doubleValue();
        final double[] radiances = new double[numberData.getElementCount()];
        for (int i = 0; i < radiances.length; i++) {
            radiances[i] = numberData.getNumber(i).doubleValue() / factor;
        }

        return radiances;
    }

    protected static EpsMetaData getMetaData(EpsFile sounderFile, String sequenceName) throws IOException {
        final CompoundData compoundData = getCompoundData(sounderFile, 0);
        final CompoundType compoundType = compoundData.getCompoundType();
        final CompoundMember compoundMember = compoundType.getMember(compoundType.getMemberIndex(sequenceName));

        return (EpsMetaData) compoundMember.getMetadata();
    }

    protected static Number getScalingFactor(EpsFile sounderFile, String sequenceName) throws IOException {
        try {
            return Double.valueOf(getMetaData(sounderFile, sequenceName).getScalingFactor().replace("10^", "1.0E"));
        } catch (NumberFormatException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    protected static NumberData getNumberData(EpsFile sounderFile, String sequenceName, Ifov ifov) throws IOException {
        return NumberData.of(getSequenceData(sounderFile, sequenceName, ifov));
    }

    protected static SequenceData getSequenceData(EpsFile sounderFile, String sequenceName, Ifov ifov) throws IOException {
        return getCompoundData(sounderFile, ifov.getMdrIndex()).getSequence(sequenceName).getSequence(
                ifov.getIfovInMdrIndex());
    }

    protected static CompoundData getCompoundData(EpsFile sounderFile, int mdrIndex) throws IOException {
        final SequenceData data = sounderFile.getMdrData();
        if (data != null) {
            return data.getCompound(mdrIndex).getCompound(1);
        }

        throw new IOException("No MDR.");
    }

    protected static class AngularRelation {
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
