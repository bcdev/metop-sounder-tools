/* 
 * Copyright (C) 2002-2007 by Brockmann Consult
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
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.jidesoft.grid.ColorCellRenderer;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.*;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.metop.iasi.IasiFile;
import org.eumetsat.metop.iasi.IasiFile.Geometry;
import org.eumetsat.metop.iasi.IasiFile.RadianceAnalysis;
import org.eumetsat.metop.iasi.IasiLayer;
import org.eumetsat.metop.iasi.IasiOverlay;
import org.eumetsat.metop.sounder.Ifov;
import org.eumetsat.metop.sounder.SounderOverlay;
import org.eumetsat.metop.sounder.SounderOverlayListener;
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
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.text.NumberFormat;

/**
 * Tool view for showing information on the selected IASI IFOV.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class IasiInfoView extends AbstractToolView {

    private static final String NO_IFOV_SELECTED = "No IFOV selected";
    private static final String IO_ERROR = "IO error";

    private static final int[] CLASS_COLORS = {
            0, Color.YELLOW.getRGB(), Color.ORANGE.getRGB(), Color.RED.getRGB(),
            Color.GREEN.getRGB(), Color.BLUE.getRGB(), Color.MAGENTA.getRGB(), Color.BLACK.getRGB()
    };

    private static final IndexColorModel CLASS_COLOR_MODEL = new IndexColorModel(8, 8, CLASS_COLORS, 0,
                                                                                 DataBuffer.TYPE_USHORT, null);

    private IasiOverlay currentOverlay;
    private IasiOverlayListener overlayListener = new IasiOverlayListener();

    private XYSeriesCollection spectrumDataset;
    private RadianceAnalysisTableModel radianceTableModel;
    private JLabel imageLabel;

    private JTextField latTextField;
    private JTextField lonTextField;
    private JTextField mdrTextField;
    private JTextField efovTextField;
    private JTextField ifovTextField;
    private JTextField szaTextField;
    private JTextField saaTextField;
    private JTextField vzaTextField;
    private JTextField vaaTextField;
    private ImageInfoEditor editor;
    private XYPlot spectrumPlot;

    @Override
    protected JComponent createControl() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Sounder Info", createInfoComponent());
        tabbedPane.add("Sounder Spectrum", createSpectrumChartComponent());
        tabbedPane.add("Radiance Analysis", createRadianceAnalysisComponent());
        tabbedPane.add("Sounder Layer", createSounderLayerComponent());

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpKey(tabbedPane, getDescriptor().getHelpId());
        }
        
        InternalFrameListener internalFrameListener = new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                final IasiLayer layer = getIasiLayer();
                if (layer != null) {
                    modelChanged(layer);
                } else {
                    final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
                    final LayerListener layerListener = new AbstractLayerListener() {
                        @Override
                        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
                            final IasiLayer layer = getIasiLayer();
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
        if (IasiFootprintVPI.isValidAvhrrProductSceneViewSelected()) {
            final IasiLayer layer = getIasiLayer();
            if (layer != null) {
                modelChanged(layer);
            }
        }

        final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"), false);
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(tabbedPane, getDescriptor().getHelpId());
        }

        final JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(tabbedPane, BorderLayout.CENTER);
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(helpButton, BorderLayout.EAST);
        containerPanel.add(buttonPanel, BorderLayout.SOUTH);

        return containerPanel;
    }
    
    private void modelChanged(IasiLayer layer) {
        currentOverlay = layer.getOverlay();
        currentOverlay.addListener(overlayListener);
        final int channel = layer.getSelectedChannel();
        final double crosshairValue = currentOverlay.channelToCrosshairValue(channel);
        spectrumPlot.setDomainCrosshairValue(crosshairValue);
        editor.setModel(createImageInfoEditorModel(layer));
        updateUI(currentOverlay.getSelectedIfov());
    }

    @Override
    public void componentFocusGained() {
        ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (IasiFootprintVPI.isValidAvhrrProductSceneView(productSceneView)) {
            IasiLayer layer = getIasiLayer();
            if (layer != null) {
                productSceneView.setSelectedLayer(layer);
            }
        }
    }

    private Component createSounderLayerComponent() {
        editor = new ImageInfoEditor();
        final JPanel containerPanel = new JPanel(new BorderLayout(4, 4));
        containerPanel.add(editor, BorderLayout.NORTH);
        return containerPanel;
    }

    private IasiLayer getIasiLayer() {
        return IasiFootprintVPI.getActiveFootprintLayer(IasiLayer.class);
    }

    private ImageInfoEditorModel createImageInfoEditorModel(final IasiLayer layer) {
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

    private Component createInfoComponent() {
        latTextField = new JTextField();
        latTextField.setEditable(false);
        lonTextField = new JTextField();
        lonTextField.setEditable(false);
        mdrTextField = new JTextField();
        mdrTextField.setEditable(false);
        efovTextField = new JTextField();
        efovTextField.setEditable(false);
        ifovTextField = new JTextField();
        ifovTextField.setEditable(false);
        szaTextField = new JTextField();
        szaTextField.setEditable(false);
        saaTextField = new JTextField();
        saaTextField.setEditable(false);
        vzaTextField = new JTextField();
        vzaTextField.setEditable(false);
        vaaTextField = new JTextField();
        vaaTextField.setEditable(false);

        TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 0.8);
        layout.setColumnWeightX(2, 1.0);
        layout.setTablePadding(3, 3);
        JPanel panel = new JPanel(layout);

        panel.add(new JLabel("Latitude:"));
        panel.add(latTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("Longitude:"));
        panel.add(lonTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("MDR index:"));
        panel.add(mdrTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("Efov index:"));
        panel.add(efovTextField);
        panel.add(new JLabel(""));

        panel.add(new JLabel("Ifov index:"));
        panel.add(ifovTextField);
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

        cleanLocationFields(NO_IFOV_SELECTED);
        
        JPanel jPanel = new JPanel(new BorderLayout(4, 4));
        jPanel.add(panel, BorderLayout.NORTH);

        return jPanel;
    }

    private Component createRadianceAnalysisComponent() {
        BorderLayout layout = new BorderLayout(4, 4);
        JPanel panel = new JPanel(layout);

        radianceTableModel = new RadianceAnalysisTableModel();
        JTable radianceTable = new RadianceAnalysisTable(radianceTableModel);
        radianceTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        JScrollPane scrollPane = new JScrollPane(radianceTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        imageLabel = new JLabel();
        panel.add(imageLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent createSpectrumChartComponent() {
        spectrumDataset = new XYSeriesCollection();

        final JFreeChart chart = ChartFactory.createXYLineChart(
                "IASI IFOV Spectrum",         // chart title
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
            final XYPlot plot = event.getChart().getXYPlot();
            final double value = plot.getDomainCrosshairValue();
            if (value > 0.0) {
                final IasiLayer layer = getIasiLayer();
                if (layer != null) {
                    final int channel = layer.getOverlay().crosshairValueToChannel(value);

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

    private void configureSpectrumChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.white);
    }

    private void configureSpectrumPlot(XYPlot plot) {
        plot.setBackgroundPaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        plot.setDomainCrosshairVisible(true);
        plot.setDomainCrosshairLockedOnData(true);
        plot.setRangeCrosshairVisible(false);
        plot.setNoDataMessage(NO_IFOV_SELECTED);
    }

    private void configureSpectrumPlotRenderer(XYLineAndShapeRenderer renderer) {
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        renderer.setBaseShapesVisible(false);
    }

    private void configureSpectrumPlotXAxis(NumberAxis axis) {
        axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        axis.setLabel("Wavenumber (cm-1)");
        axis.setRange(new Range(645.0, 2760.0), true, false);
    }

    private void configureSpectrumPlotYAxis(NumberAxis axis) {
        axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        axis.setLabel("Brightness Temperature (K)");
        axis.setRange(new Range(180.0, 320.0), true, false);
    }

    private void configureSpectrumChartPanel(ChartPanel chartPanel) {
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawHeight(20000);
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMaximumDrawWidth(20000);
        chartPanel.setPreferredSize(new Dimension(400, 200));
    }

    private void updateUI(Ifov selectedIfov) {
        int ifovId = -1;
        if (currentOverlay != null && selectedIfov != null) {
            ifovId = selectedIfov.getIfovIndex();
        }
        updateLocation(ifovId);
        updateSpectrum(ifovId);
        updateRadianceAnalysis(ifovId);
    }

    private void updateLocation(int ifovId) {
        if (ifovId == -1) {
            cleanLocationFields(NO_IFOV_SELECTED);
            return;
        }
        final GeoPos geoPos;
        final Geometry readGeometry;
        try {
            IasiFile iasiFile = currentOverlay.getEpsFile();
            geoPos = iasiFile.readGeoPos(ifovId);
            readGeometry = iasiFile.readGeometry(ifovId);
        } catch (IOException e) {
            cleanLocationFields(IO_ERROR);
            return;
        }
        lonTextField.setText(geoPos.getLonString());
        latTextField.setText(geoPos.getLatString());

        vzaTextField.setText(Double.toString(readGeometry.vza));
        vaaTextField.setText(Double.toString(readGeometry.vaa));
        szaTextField.setText(Double.toString(readGeometry.sza));
        saaTextField.setText(Double.toString(readGeometry.saa));

        efovTextField.setText(Integer.toString(IasiFile.computeEfovIndex(ifovId)));
        mdrTextField.setText(Integer.toString(IasiFile.computeMdrIndex(ifovId)));
        ifovTextField.setText(Integer.toString(IasiFile.computeIfovIndex(ifovId)));
    }

    private void cleanLocationFields(String text) {
        lonTextField.setText(text);
        latTextField.setText(text);
        saaTextField.setText(text);
        vaaTextField.setText(text);
        szaTextField.setText(text);
        vzaTextField.setText(text);
        mdrTextField.setText(text);
        efovTextField.setText(text);
        ifovTextField.setText(text);
    }


    private void updateRadianceAnalysis(int ifovId) {
        if (ifovId == -1) {
            cleanRadianceAnalysis();
            return;
        }
        final RadianceAnalysis radianceAnalysis;
        try {
            radianceAnalysis = currentOverlay.getEpsFile().readRadianceAnalysis(ifovId);
        } catch (IOException e) {
            cleanRadianceAnalysis();
            return;
        }
        radianceTableModel.setRadianceAnalysis(radianceAnalysis);
        byte[] imageData = new byte[radianceAnalysis.imageW * radianceAnalysis.imageH];
        int i = 0;
        for (int y = 0; y < radianceAnalysis.imageH; y++) {
            for (int x = 0; x < radianceAnalysis.imageW; x++) {
                imageData[i++] = radianceAnalysis.image[y][x];
            }
        }
        BufferedImage bufferedImage = ImageUtils.createIndexedImage(radianceAnalysis.imageW,
                                                                    radianceAnalysis.imageH, imageData,
                                                                    CLASS_COLOR_MODEL);
        imageLabel.setVisible(true);
        imageLabel.setIcon(new ImageIcon(bufferedImage));
    }

    private void cleanRadianceAnalysis() {
        radianceTableModel.setRadianceAnalysis(null);
        imageLabel.setVisible(false);
    }

    private void updateSpectrum(int ifovId) {
        spectrumDataset.removeAllSeries();
        if (ifovId == -1) {
            return;
        }

        double[][] spectrum;
        try {
            spectrum = readBrightnessTemperatureSpectrum(ifovId);
        } catch (IOException e) {
            return;
        }
        XYSeries series = new XYSeries("Sample Values");
        for (double[] aSpectrum : spectrum) {
            series.add(aSpectrum[0] / 100.0, aSpectrum[1]);
        }
        spectrumDataset.addSeries(series);
    }

    private class IasiOverlayListener implements SounderOverlayListener {
        @Override
        public void selectionChanged(SounderOverlay overlay) {
            updateUI(overlay.getSelectedIfov());
        }

        @Override
        public void dataChanged(SounderOverlay overlay) {
            updateUI(overlay.getSelectedIfov());
        }
    }

    private double[][] readBrightnessTemperatureSpectrum(int ifovId) throws IOException {
        final double[][] spectrum = currentOverlay.getEpsFile().readSpectrum(ifovId);

        for (double[] sample : spectrum) {
            sample[1] = BlackBody.temperatureAtWavenumber(sample[0], sample[1]);
        }

        return spectrum;
    }

    private static class RadianceAnalysisTableModel extends DefaultTableModel {

        private static final String[] rowNames = new String[]{
                "Color", "Coverage",
                "Gravity Y", "Gravity Z",
                "Radiance_1",
                "Radiance_2",
                "Radiance_3a",
                "Radiance_3b",
                "Radiance_4",
                "Radiance_5",
        };

        private static final String[] columnNames = new String[]{
                "", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", " No Class"
        };
        private static final Color[] colors = new Color[]{
                Color.YELLOW, Color.ORANGE, Color.RED,
                Color.GREEN, Color.BLUE, Color.MAGENTA,
                Color.BLACK
        };

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        private RadianceAnalysis radAnalysis;
        private NumberFormat numberFormat;
        private NumberFormat percentFormat;

        private RadianceAnalysisTableModel() {
            numberFormat = NumberFormat.getNumberInstance();
            numberFormat.setMaximumFractionDigits(3);
            numberFormat.setMinimumFractionDigits(3);
            percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(2);
            percentFormat.setMinimumFractionDigits(2);
            percentFormat.setParseIntegerOnly(false);

        }

        public void setRadianceAnalysis(RadianceAnalysis radAnalysis) {
            if (this.radAnalysis != radAnalysis) {
                this.radAnalysis = radAnalysis;
                fireTableDataChanged();
            }
        }

        @Override
        public int getRowCount() {
            return rowNames.length;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {

            if (columnIndex == 0) {
                return rowNames[rowIndex];
            }
            if (rowIndex == 0) {
                return colors[columnIndex - 1];
            }
            if (radAnalysis != null) {
                switch (rowIndex) {
                    case 1:
                        return percentFormat.format(radAnalysis.wgt[columnIndex - 1]);
                    case 2:
                        return numberFormat.format(radAnalysis.y[columnIndex - 1]);
                    case 3:
                        return numberFormat.format(radAnalysis.z[columnIndex - 1]);
                    case 4: // 0
                    case 5: // 1
                    case 6: // 2
                    case 7:// 3
                    case 8:// 4
                    case 9:// 5
                        String meanString = numberFormat.format(radAnalysis.mean[columnIndex - 1][(rowIndex - 4)]);
                        String stdDev = numberFormat.format(radAnalysis.std[columnIndex - 1][(rowIndex - 4)]);
                        return meanString + " \261 " + stdDev;
                    default:
                        return "";
                }
            } else {
                return "";
            }
        }

    }

    private class RadianceAnalysisTable extends JTable {

        private final ColorCellRenderer colorRenderer;

        public RadianceAnalysisTable(RadianceAnalysisTableModel radianceTableModel) {
            colorRenderer = new ColorCellRenderer();
            colorRenderer.setColorValueVisible(false);
            setModel(radianceTableModel);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if (row == 0 && column >= 1) {
                return colorRenderer;
            }
            return super.getCellRenderer(row, column);
        }
    }
}
