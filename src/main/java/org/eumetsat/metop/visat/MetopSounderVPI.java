/*
 * $Id: MetopSounderVPI.java,v 1.38 2005/12/22 10:05:29 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.eumetsat.metop.visat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatPlugIn;
import org.eumetsat.metop.amsu.AmsuFile;
import org.eumetsat.metop.amsu.AmsuSounderLayer;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.eps.EpsFormats;
import org.eumetsat.metop.iasi.IasiFile;
import org.eumetsat.metop.iasi.IasiLayer;
import org.eumetsat.metop.mhs.MhsFile;
import org.eumetsat.metop.mhs.MhsSounderLayer;
import org.eumetsat.metop.sounder.AvhrrOverlay;
import org.eumetsat.metop.sounder.SounderLayer;

import java.awt.Container;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileFilter;

import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.glayer.Layer;
import com.jidesoft.action.DockableBar;

public class MetopSounderVPI implements VisatPlugIn {

    public static final String[] METOP_AVHRR_SIGNATURE = {
            "latitude",
            "longitude",
            "view_zenith",
            "sun_zenith",
            "delta_azimuth",
            "radiance_1",
            "radiance_2",
            "radiance_3a",
            "radiance_3b",
            "radiance_4",
            "radiance_5",
    };

    private static final FileFilter IASI_NAME_FILTER = new PatternFileFilter("IASI");
    private static final FileFilter AMSU_NAME_FILTER = new PatternFileFilter("AMSU");
    private static final FileFilter MHS_NAME_FILTER = new PatternFileFilter("MHSx");

    private static MetopSounderVPI instance;
    private VisatApp visatApp;
    private static final String FOOTPRINT_CHOOSE_OPTION_NAME = "iasi.file.chooser";
    private Map<Product, AvhrrOverlay> iasiFootprintLayerModelMap;
    private Map<Product, AvhrrOverlay> amsuFootprintLayerModelMap;
    private Map<Product, AvhrrOverlay> mhsFootprintLayerModelMap;

    // called via reflection
    public MetopSounderVPI() {
        if (instance == null) {
            instance = this;
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // VisatPlugIn interface implementation

    @Override
    public void start(final VisatApp visatApp) {
        this.visatApp = visatApp;
        iasiFootprintLayerModelMap = new HashMap<Product, AvhrrOverlay>(11);
        amsuFootprintLayerModelMap = new HashMap<Product, AvhrrOverlay>(11);
        mhsFootprintLayerModelMap = new HashMap<Product, AvhrrOverlay>(11);
        registerProductTreeListener();
        registerInternalFrameHandler();

        ExtensionManager.getInstance().register(IasiLayer.class, new IasiLayer.LayerUIFactory());
        ExtensionManager.getInstance().register(SounderLayer.class, new SounderLayer.LayerUIFactory());
    }

    public void addCommandsToToolBar(VisatApp myVisatApp, DockableBar toolBar, String commandID) {
        final Command command = myVisatApp.getCommandManager().getCommand(commandID);
        assert command != null : "command != null [commandID=".concat(commandID).concat("]");
        final AbstractButton toolBarButton = command.createToolBarButton();
        toolBarButton.addMouseListener(myVisatApp.getMouseOverActionHandler());
        toolBar.add(toolBarButton);
        toolBar.add(Box.createHorizontalStrut(1));
    }

    @Override
    public void stop(VisatApp visatApp) {
    }

    /**
     * Tells a plug-in to update its component tree (if any) since the Java look-and-feel has changed.
     * <p/>
     * <p>If a plug-in uses top-level containers such as dialogs or frames, implementors of this method should invoke
     * <code>SwingUtilities.updateComponentTreeUI()</code> on such containers.
     */
    @Override
    public void updateComponentTreeUI() {
    }

    // VisatPlugIn interface implementation
    /////////////////////////////////////////////////////////////////////////

    public static MetopSounderVPI getInstance() {
        return instance;
    }

    public VisatApp getVisatApp() {
        return visatApp;
    }

    public static boolean isValidAvhrrProductSceneViewSelected() {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        return view != null && isValidAvhrrProduct(view.getProduct());
    }

    public static boolean isValidAvhrrProductSceneView(final ProductSceneView view) {
        return view != null && isValidAvhrrProduct(view.getProduct());
    }

    public static boolean isValidAvhrrProduct(final Product product) {
        if (product == null) {
            return false;
        }
        if (product.getGeoCoding() == null) {
            return false;
        }
        if (product.getFileLocation() == null) {
            return false;
        }
        for (String rdnName : METOP_AVHRR_SIGNATURE) {
            final RasterDataNode rasterDataNode = product.getRasterDataNode(rdnName);
            if (rasterDataNode == null) {
                return false;
            }
        }
        return true;
    }

    public static synchronized ProductSceneView getProductSceneView(InternalFrameEvent e) {
        final JInternalFrame internalFrame = e.getInternalFrame();
        final Container contentPane = internalFrame.getContentPane();
        if (contentPane instanceof ProductSceneView) {
            return (ProductSceneView) contentPane;
        }
        return null;
    }

    public static Layer getRootLayer(InternalFrameEvent e) {
        final JInternalFrame internalFrame = e.getInternalFrame();
        final Container contentPane = internalFrame.getContentPane();

        if (!(contentPane instanceof ProductSceneView)) {
            return null;
        }

        final ProductSceneView view = (ProductSceneView) contentPane;
        return view.getRootLayer();
    }

    public static <T extends Layer> T getFootprintLayer(ProductSceneView view, Class<T> layerType) {
        return getLayer(view.getRootLayer(), layerType);
    }

    public static <T extends Layer> T getActiveFootprintLayer(Class<T> layerType) {
        final MetopSounderVPI vpi = getInstance();
        final VisatApp app = vpi.getVisatApp();
        final ProductSceneView psv = app.getSelectedProductSceneView();
        if (psv == null) {
            return null;
        }
        Layer rootLayer = psv.getRootLayer();
        return getLayer(rootLayer, layerType);
    }

    /////////////////////////////////////////////////////////////////////////
    // Private stuff

    private synchronized void registerProductTreeListener() {
        visatApp.addProductTreeListener(new ProductTreeHandler());
    }

    private synchronized void registerInternalFrameHandler() {
        visatApp.addInternalFrameListener(new ProductSceneViewHook());
    }

    private synchronized void addFootprintLayers(ProductSceneView psv) {
        Layer rootLayer = psv.getRootLayer();
        final Product avhrrProduct = psv.getProduct();
        final File avhrrFileLocation = avhrrProduct.getFileLocation();
        if (avhrrFileLocation == null) {
            // might be a subset - subsets are not yet supported
            return;
        }
        final String avhrrFilename = avhrrFileLocation.getName();
        final File avhrrDir = avhrrFileLocation.getParentFile();

        long avhrrStartTime = 0;
        try {
            avhrrStartTime = IasiFile.extractStartTimeInMillis(avhrrFilename);
        } catch (Exception e) {
            // wrong filename, do not try to add overlay
            return;
        }
        AvhrrProductInfo avhrrInfo = new AvhrrProductInfo(avhrrProduct, avhrrFilename, avhrrDir, avhrrStartTime);

        FilenameFilter iasiTimeFilter = new IasiFile.NameFilter(avhrrInfo.avhrrFilename);
        FilenameFilter amsuTimeFilter = new AmsuFile.NameFilter(avhrrInfo.avhrrFilename);
        FilenameFilter mhsTimeFilter = new MhsFile.NameFilter(avhrrInfo.avhrrFilename);

        addOverlayLayer(rootLayer, avhrrInfo, amsuTimeFilter, AMSU_NAME_FILTER, AmsuSounderLayer.class,
                        amsuFootprintLayerModelMap, "AMSU");
        addOverlayLayer(rootLayer, avhrrInfo, mhsTimeFilter, MHS_NAME_FILTER, MhsSounderLayer.class,
                        mhsFootprintLayerModelMap, "MHS");
        addOverlayLayer(rootLayer, avhrrInfo, iasiTimeFilter, IASI_NAME_FILTER, IasiLayer.class,
                        iasiFootprintLayerModelMap, "IASI");
    }

    private static <T extends Layer> T getLayer(Layer rootLayer, Class<T> layerType) {
        if (layerType.isAssignableFrom(rootLayer.getClass())) {
            return (T) rootLayer;
        }
        for (Layer childLayer : rootLayer.getChildren()) {
            T layer = getLayer(childLayer, layerType);
            if (layer != null) {
                return layer;
            }
        }
        return null;
    }

    private static <T extends Layer> boolean hasLayer(Layer layer, Class<T> layerType) {
        if (layerType.isAssignableFrom(layer.getClass())) {
            return true;
        }
        for (Layer childLayer : layer.getChildren()) {
            if (hasLayer(childLayer, layerType)) {
                return true;
            }
        }
        return false;
    }

    private static <T extends Layer> void removeLayer(Layer layer, Class<T> layerType) {
        List<Layer> children = layer.getChildren();
        for (Layer childLayer : children) {
            if (childLayer.getClass().isAssignableFrom(layerType)) {
                children.remove(childLayer);
                return;
            }
        }
    }

    private void addOverlayLayer(final Layer rootLayer, AvhrrProductInfo avhrrInfo, FilenameFilter timeFilter, FileFilter nameFilter, Class<? extends Layer> layerType, Map<Product, AvhrrOverlay> overlayMap, String type) {
        if (!hasLayer(rootLayer, layerType)) {
            AvhrrOverlay overlay = overlayMap.get(avhrrInfo.avhrrProduct);
            if (overlay == null) {
                EpsFile epsFile = openEpsFile(avhrrInfo, timeFilter, nameFilter, type);
                if (epsFile != null) {
                    overlay = epsFile.createOverlay(avhrrInfo.avhrrProduct);
                    overlayMap.put(avhrrInfo.avhrrProduct, overlay);
                }
            }
            if (overlay != null) {
                final EpsFile epsFile = overlay.getEpsFile();
                final AvhrrOverlay theOverlay = overlay;
                SwingWorker<Layer, Void> createLayerWorker = new SwingWorker<Layer, Void>() {

                    @Override
                    protected Layer doInBackground() throws Exception {
                        return epsFile.createLayer(theOverlay);
                    }

                    @Override
                    protected void done() {
                        Layer layer = null;
                        try {
                            layer = get();
                        } catch (Exception e) {
                            return;
                        }
                        if (layer != null) {
                            rootLayer.getChildren().add(0, layer);
                            // TODO (mz, 11,11,2008) HACK, because there is something wrong with the change listener
                            layer.setVisible(false);
                            layer.setVisible(true);
                        }
                    }
                };
                createLayerWorker.execute();
            }
        }
    }

    private EpsFile openEpsFile(AvhrrProductInfo avhrrInfo, FilenameFilter timeFilter, FileFilter nameFilter, String type) {
        File file = EpsFile.findFile(avhrrInfo.avhrrStartTime, avhrrInfo.avhrrDir.listFiles(timeFilter));
        if (file == null) {
            if (visatApp.showQuestionDialog(type + " Footprint Layer",
                                            "No matching " + type + " file was found for this AVHRR scene.\n" +
                                                    "Do you want to choose one manually?",
                                            FOOTPRINT_CHOOSE_OPTION_NAME) == JOptionPane.YES_OPTION) {
                file = showOpenFileDialog("Open " + type + " File", nameFilter, avhrrInfo.avhrrDir);
            }
        }
        if (file == null) {
            return null;
        }
        try {
            return EpsFormats.getInstance().openFile(file);
        } catch (IOException e) {
            visatApp.showErrorDialog(type + " Footprint Layer",
                                     "Not able to create " + type + " Footprint layer.");
            return null;
        }
    }

    private File showOpenFileDialog(String title, FileFilter fileFilter, File currentDir) {
        BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }
        fileChooser.setDialogTitle(visatApp.getAppName() + " - " + title);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(visatApp.getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file == null || "".equals(file.getName())) {
                return null;
            }
            file = file.getAbsoluteFile();
            return file;
        }
        return null;
    }

    private static <T extends Layer> void removeLayer(ProductSceneView psv, Class<T> layerType) {
        Layer rootLayer = psv.getRootLayer();
        boolean hasLayer = hasLayer(rootLayer, layerType);
        if (hasLayer) {
            removeLayer(rootLayer, layerType);
        }
    }

    private static void removeOverlay(Product avhrrProduct, Map<Product, AvhrrOverlay> overlayMap) {
        AvhrrOverlay overlay = overlayMap.get(avhrrProduct);
        if (overlay != null) {
            EpsFile epsFile = overlay.getEpsFile();
            epsFile.close();
            overlayMap.remove(avhrrProduct);
        }
    }

    private static class AvhrrProductInfo {
        private final Product avhrrProduct;
        private final String avhrrFilename;
        private final File avhrrDir;
        private final long avhrrStartTime;

        private AvhrrProductInfo(Product avhrrProduct, String avhrrFilename, File avhrrDir, long avhrrStartTime) {
            this.avhrrProduct = avhrrProduct;
            this.avhrrFilename = avhrrFilename;
            this.avhrrDir = avhrrDir;
            this.avhrrStartTime = avhrrStartTime;
        }
    }

    private static class PatternFileFilter extends FileFilter {
        private final String matchExpression;
        private final String description;

        public PatternFileFilter(String typeIdentifier) {
            this.matchExpression = typeIdentifier + "_.*.nat";
            this.description = typeIdentifier + " Files (*.nat)";
        }

        @Override
        public boolean accept(File f) {
            return f.getName().matches(matchExpression);
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    private class ProductSceneViewHook extends InternalFrameAdapter {
        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            final ProductSceneView psv = getProductSceneView(e);
            if (isValidAvhrrProductSceneView(psv)) {
                addFootprintLayers(psv);
                psv.repaint();
            }
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final ProductSceneView psv = getProductSceneView(e);
            if (isValidAvhrrProductSceneView(psv)) {
                removeLayer(psv, IasiLayer.class);
                removeLayer(psv, AmsuSounderLayer.class);
                removeLayer(psv, MhsSounderLayer.class);
            }
        }
    }

    private class ProductTreeHandler implements ProductTreeListener {
        @Override
        public void productAdded(Product product) {
        }

        @Override
        public void productRemoved(Product avhrrProduct) {
            removeOverlay(avhrrProduct, iasiFootprintLayerModelMap);
            removeOverlay(avhrrProduct, amsuFootprintLayerModelMap);
            removeOverlay(avhrrProduct, mhsFootprintLayerModelMap);
        }

        @Override
        public void productSelected(Product product, int clickCount) {
        }

        @Override
        public void metadataElementSelected(MetadataElement group, int clickCount) {
        }

        @Override
        public void tiePointGridSelected(TiePointGrid tiePointGrid, int clickCount) {
        }

        @Override
        public void bandSelected(Band band, int clickCount) {
        }
    }
}