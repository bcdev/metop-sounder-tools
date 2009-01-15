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
import com.bc.ceres.binio.SequenceData;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.iasi.footprint.IasiFootprintLayer;
import org.eumetsat.metop.amsu.AmsuSounderLayer;
import org.eumetsat.metop.iasi.IasiFile;
import org.eumetsat.metop.sounder.SounderFile;
import org.eumetsat.metop.sounder.SounderLayer;
import org.eumetsat.metop.sounder.SounderIfov;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;

/**
 * Tool view for showing information on the selected AMSU field-of-view.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
public class AmsuInfoView extends AbstractToolView {

    private static final String NO_DATA_MESSAGE = "no data";

    private SounderFile sounderFile;
    private SounderLayerSelectionListener layerSelectionListener;

    private XYSeriesCollection dataset;

    private JTextField latTextField;
    private JTextField lonTextField;
    private JTextField mdrTextField;
    private JTextField ifovTextField;
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
        tabbedPane.add("Sounder Spectrum", createSpectrumComponent());

        return tabbedPane;
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
        mdrTextField = new JTextField();
        ifovTextField = new JTextField();
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

        final JPanel containerPanel = new JPanel(layout);

        containerPanel.add(new JLabel("Latitude:"));
        containerPanel.add(latTextField);
        containerPanel.add(new JLabel(""));

        containerPanel.add(new JLabel("Longitude:"));
        containerPanel.add(lonTextField);
        containerPanel.add(new JLabel(""));

        containerPanel.add(new JLabel("MDR index:"));
        containerPanel.add(mdrTextField);
        containerPanel.add(new JLabel(""));

        containerPanel.add(new JLabel("Ifov index:"));
        containerPanel.add(ifovTextField);
        containerPanel.add(new JLabel(""));

        containerPanel.add(new JLabel("Sun zenith:"));
        containerPanel.add(szaTextField);
        containerPanel.add(new JLabel(""));

        containerPanel.add(new JLabel("Sun azimuth:"));
        containerPanel.add(saaTextField);
        containerPanel.add(new JLabel(""));

        containerPanel.add(new JLabel("View zenith:"));
        containerPanel.add(vzaTextField);
        containerPanel.add(new JLabel(""));

        containerPanel.add(new JLabel("View azimuth:"));
        containerPanel.add(vaaTextField);
        containerPanel.add(new JLabel(""));

        final JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(containerPanel, BorderLayout.NORTH);

        return panel;
    }

    // todo - revise
    private JFreeChart createXYLineChart() {
        NumberAxis xAxis = new NumberAxis("Wavenumber (cm-1)");
        xAxis.setRange(645.0, 2760.0);
        NumberAxis yAxis = new NumberAxis("Scene Radiance (mW/m2/sr/cm-1)");
        yAxis.setRange(new Range(180.0, 305.0));
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setNoDataMessage(NO_DATA_MESSAGE);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        final boolean legend = false;
        return new JFreeChart("IASI IFOV Spectrum", JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    }

    // todo - revise
    private JComponent createSpectrumComponent() {
        BorderLayout layout = new BorderLayout(4, 4);
        JPanel panel = new JPanel(layout);

        dataset = new XYSeriesCollection();
        JFreeChart chart = createXYLineChart();
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawHeight(20000);
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMaximumDrawWidth(20000);
        chartPanel.setPreferredSize(new Dimension(200, 200));
        panel.add(chartPanel);

        return panel;
    }

    private void update(SounderIfov[] selectedIfovs) {
        if (sounderFile != null) {
            int ifovIndex = -1;
            if (selectedIfovs.length != 0) {
                ifovIndex = selectedIfovs[0].ifovInMdrIndex;
            }
            updateInfoFields(ifovIndex);
            updateSpectrumDataset(ifovIndex);
        }
    }

    private void updateInfoFields(int ifovId) {
        if (ifovId == -1) {
            clearInfoFields();
            return;
        }
        final GeoPos geoPos;
        final SounderAngularRelation angularRelation;
        try {
            geoPos = readEarthLocation(sounderFile, ifovId);
            angularRelation = readAngularRelation(sounderFile, ifovId);
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

        mdrTextField.setText(Integer.toString(IasiFile.computeMdrIndex(ifovId)));
        ifovTextField.setText(Integer.toString(IasiFile.computeIfovIndex(ifovId)));
    }

    private void clearInfoFields() {
        lonTextField.setText("");
        latTextField.setText("");
        saaTextField.setText("");
        vaaTextField.setText("");
        szaTextField.setText("");
        vzaTextField.setText("");
        mdrTextField.setText("");
        ifovTextField.setText("");
    }

    // todo - revise
    private void updateSpectrumDataset(int ifovId) {
        dataset.removeAllSeries();
        if (ifovId == -1) {
            return;
        }

        double[][] spectrum;
        try {
            spectrum = readSceneSpectrum(sounderFile, ifovId);
        } catch (IOException e) {
            return;
        }
        XYSeries series = new XYSeries("Sample Values");
        for (double[] sample : spectrum) {
            series.add(sample[0] / 100.0, sample[1]);
        }
        dataset.addSeries(series);
    }

    private class SounderLayerSelectionListenerImpl implements SounderLayerSelectionListener {

        @Override
        public void selectionChanged(SounderLayer layer) {
            // todo - implement (rq-20090114)
            // update(layer.getSelectedIfovIds());
        }

    }

    private class ProductSceneViewHook extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final ProductSceneView psv = VisatApp.getApp().getSelectedProductSceneView();
            if (IasiFootprintVPI.isValidAvhrrProductSceneView(psv)) {
                final SounderLayer layer = IasiFootprintVPI.getActiveFootprintLayer(AmsuSounderLayer.class);
                if (layer != null) {
                    layerSelectionListener = new SounderLayerSelectionListenerImpl();
                    // todo - discuss with mz
                    // layer.getModel().addListener(modelListener);
                    // sounderFile = layer.getSounderOverlay().getSounderFile();
                }
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final ProductSceneView psv = VisatApp.getApp().getSelectedProductSceneView();
            if (IasiFootprintVPI.isValidAvhrrProductSceneView(psv)) {
                final SounderLayer layer = IasiFootprintVPI.getActiveFootprintLayer(AmsuSounderLayer.class);
                if (layer != null) {
                    // todo - discuss with mz
                    // layer.getModel().removeListener(modelListener);
                    sounderFile = null;
                }
            }
        }
    }

    private static SounderAngularRelation readAngularRelation(SounderFile sounderFile, int ifovId) throws IOException {
        final SequenceData mdrData = sounderFile.getMdrData();

        // todo - implement (rq-20090114)
        return new SounderAngularRelation();
    }

    private static GeoPos readEarthLocation(SounderFile sounderFile, int ifovId) throws IOException {
        // todo - implement (rq-20090114)
        return new GeoPos();
    }

    private static double[][] readSceneSpectrum(SounderFile sounderFile, int ifovId) throws IOException {
        // todo - implement (rq-20090114)
        return new double[0][0];
    }

    // todo - move up (rq-20090114)
    public interface SounderLayerSelectionListener {
        void selectionChanged(SounderLayer layer);
    }

    // todo - move up (rq-20090114)
    public static class SounderAngularRelation {
        // solar-zenith, sat-zenith, solar-azimuth, sat-azimuth
        public double sza;
        public double vza;
        public double saa;
        public double vaa;
    }
}
