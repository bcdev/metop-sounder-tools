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

import com.jidesoft.grid.ColorCellRenderer;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.metop.iasi.IasiFile;
import org.eumetsat.metop.iasi.IasiLayer;
import org.eumetsat.metop.iasi.IasiOverlay;
import org.eumetsat.metop.iasi.IasiOverlayListener;
import org.eumetsat.metop.iasi.Ifov;
import org.eumetsat.metop.iasi.IasiFile.Geometry;
import org.eumetsat.metop.iasi.IasiFile.RadianceAnalysis;
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.text.NumberFormat;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Tool view for showing information on the selected IASI IFOV.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class IasiInfoView extends AbstractToolView {

    private static final String NO_DATA_MESSAGE = "no data";
    private static final int[] CLASS_COLORS = {
            0, Color.YELLOW.getRGB(), Color.ORANGE.getRGB(), Color.RED.getRGB(),
            Color.GREEN.getRGB(), Color.BLUE.getRGB(), Color.MAGENTA.getRGB(), Color.BLACK.getRGB()
    };

    private static final IndexColorModel CLASS_COLOR_MODEL = new IndexColorModel(8, 8, CLASS_COLORS, 0, DataBuffer.TYPE_USHORT, null);

    private IasiFile iasiFile;
    private IasiListener modelListener;

    private XYSeriesCollection dataset;
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

    @Override
    protected JComponent createControl() {
        VisatApp.getApp().addInternalFrameListener(new ProductSceneViewHook());

        IasiLayer layer = null;
        final ProductSceneView psv = VisatApp.getApp().getSelectedProductSceneView();
        if (psv != null && IasiFootprintVPI.isValidAvhrrProductSceneView(psv)) {
            layer = IasiFootprintVPI.getActiveFootprintLayer(IasiLayer.class);
            if (layer != null) {
                modelListener = new IasiListener();
                layer.getOverlay().addListener(modelListener);
                iasiFile = layer.getOverlay().getIasiFile();
            }
        }
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Location and Geometry", createLocationComponent());
        tabbedPane.add("IASI Spectrum", createSpectrumComponent());
        tabbedPane.add("Radiance Analysis", createRadianceAnalysis());

        if (layer != null) {
            update(layer.getOverlay().getSelectedIfov());
        }
        return tabbedPane;
    }


    private Component createLocationComponent() {
        latTextField = new JTextField();
        lonTextField = new JTextField();
        mdrTextField = new JTextField();
        efovTextField = new JTextField();
        ifovTextField = new JTextField();
        szaTextField = new JTextField();
        saaTextField = new JTextField();
        vzaTextField = new JTextField();
        vaaTextField = new JTextField();

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

        JPanel jPanel = new JPanel(new BorderLayout(4, 4));
        jPanel.add(panel, BorderLayout.NORTH);

        return jPanel;
    }

    private Component createRadianceAnalysis() {
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

    private JFreeChart createXYLineChart() {
        NumberAxis xAxis = new NumberAxis("Wavenumber (cm-1)");
        xAxis.setRange(645.0, 2760.0);
        NumberAxis yAxis = new NumberAxis("Brightness Temperature (K)");
        yAxis.setRange(new Range(180.0, 305.0));
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setNoDataMessage(NO_DATA_MESSAGE);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        final boolean legend = false;
        return new JFreeChart("IASI IFOV Spectrum", JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    }

    // todo - default plot settings
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

    private void update(Ifov selectedIfov) {
        if (iasiFile != null && selectedIfov != null) {
            int ifovId = selectedIfov.getIndex();
            updateLocation(ifovId);
            updateSpectrum(ifovId);
            updateRadianceAnalysis(ifovId);
        }
    }

    private void updateLocation(int ifovId) {
        if (ifovId == -1) {
            cleanLocationFields();
            return;
        }
        final GeoPos geoPos;
        final Geometry readGeometry;
        try {
            geoPos = iasiFile.readGeoPos(ifovId);
            readGeometry = iasiFile.readGeometry(ifovId);
        } catch (IOException e) {
            cleanLocationFields();
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

    private void cleanLocationFields() {
        lonTextField.setText("");
        latTextField.setText("");
        saaTextField.setText("");
        vaaTextField.setText("");
        szaTextField.setText("");
        vzaTextField.setText("");
        mdrTextField.setText("");
        efovTextField.setText("");
        ifovTextField.setText("");
    }


    private void updateRadianceAnalysis(int ifovId) {
        final RadianceAnalysis radianceAnalysis;
        if (ifovId == -1) {
            cleanRadianceAnalysis();
            return;
        }
        try {
            radianceAnalysis = iasiFile.readRadianceAnalysis(ifovId);
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

    // todo - clean up
    private void updateSpectrum(int ifovId) {
        dataset.removeAllSeries();
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
        dataset.addSeries(series);
    }

    private class IasiListener implements IasiOverlayListener {
        @Override
        public void selectionChanged(IasiOverlay model) {
            update(model.getSelectedIfov());
        }
    }

    private class ProductSceneViewHook extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final ProductSceneView psv = VisatApp.getApp().getSelectedProductSceneView();
            if (IasiFootprintVPI.isValidAvhrrProductSceneView(psv)) {
                final IasiLayer layer = IasiFootprintVPI.getActiveFootprintLayer(IasiLayer.class);
                if (layer != null) {
                    modelListener = new IasiListener();
                    layer.getOverlay().addListener(modelListener);
                    iasiFile = layer.getOverlay().getIasiFile();
                }
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final ProductSceneView psv = VisatApp.getApp().getSelectedProductSceneView();
            if (IasiFootprintVPI.isValidAvhrrProductSceneView(psv)) {
                final IasiLayer layer = IasiFootprintVPI.getActiveFootprintLayer(IasiLayer.class);
                if (layer != null) {
                    layer.getOverlay().removeListener(modelListener);
                    iasiFile = null;
                }
            }
        }
    }

    private double[][] readBrightnessTemperatureSpectrum(int ifovId) throws IOException {
        final double[][] spectrum = iasiFile.readSpectrum(ifovId);

        for (double[] sample : spectrum) {
            sample[1] = BlackBody.temperatureForWavenumber(sample[0], sample[1]);
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
            if(this.radAnalysis != radAnalysis) {
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

            if(columnIndex == 0) {
                return rowNames[rowIndex];
            }
            if(rowIndex == 0) {
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
            }else {
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
            if(row == 0 && column >= 1) {
               return colorRenderer;
            }
            return super.getCellRenderer(row, column);
        }
    }
}
