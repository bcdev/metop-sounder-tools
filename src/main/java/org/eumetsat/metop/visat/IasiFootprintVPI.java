/*
 * $Id: IavisaVPI.java,v 1.38 2005/12/22 10:05:29 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.eumetsat.metop.visat;

import com.bc.ceres.glayer.Layer;
import com.jidesoft.action.CommandBar;
import com.jidesoft.action.DockableBar;
import com.jidesoft.action.DockableBarContext;
import com.jidesoft.action.DockableBarManager;
import com.jidesoft.action.event.DockableBarAdapter;
import com.jidesoft.action.event.DockableBarEvent;

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
import org.eumetsat.iasi.footprint.IasiFootprintLayer;
import org.eumetsat.metop.amsu.AmsuFile;
import org.eumetsat.metop.amsu.AmsuSounderLayer;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.eps.EpsFormats;
import org.eumetsat.metop.iasi.IasiFile;
import org.eumetsat.metop.mhs.MhsFile;
import org.eumetsat.metop.mhs.MhsSounderLayer;
import org.eumetsat.metop.sounder.AvhrrOverlay;

import java.awt.Container;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileFilter;

public class IasiFootprintVPI implements VisatPlugIn {

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

    private static IasiFootprintVPI instance;
    private VisatApp visatApp;
    private static final String FOOTPRINT_CHOOSE_OPTION_NAME = "iasi.file.chooser";
    private HashMap<Product, AvhrrOverlay> iasiFootprintLayerModelMap;
    private HashMap<Product, AvhrrOverlay> amsuFootprintLayerModelMap;
    private HashMap<Product, AvhrrOverlay> mhsFootprintLayerModelMap;

    // called via reflection
    public IasiFootprintVPI() {
        instance = this;
    }

    /////////////////////////////////////////////////////////////////////////
    // VisatPlugIn interface implementation

    public void start(final VisatApp visatApp) {
        this.visatApp = visatApp;
        iasiFootprintLayerModelMap = new HashMap<Product, AvhrrOverlay>(11);
        amsuFootprintLayerModelMap = new HashMap<Product, AvhrrOverlay>(11);
        mhsFootprintLayerModelMap = new HashMap<Product, AvhrrOverlay>(11);
        registerProductTreeListener();
        registerInternalFrameHandler();

        final DockableBarManager barManager = visatApp.getMainFrame().getDockableBarManager();
        DockableBar toolBar = barManager.getDockableBar("iasiToolBar");
        if (toolBar == null) {
            toolBar = createToolBar(visatApp);
            barManager.addDockableBar(toolBar);
        }
        addCommandsToToolBar(visatApp, toolBar, "showIasiOverlay");
    }

    public CommandBar createToolBar(final VisatApp visatApp) {
        final CommandBar toolBar = new CommandBar("iasiToolBar");
        toolBar.setTitle("IASI Tools");
        toolBar.getContext().setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
        toolBar.getContext().setInitIndex(1);
        toolBar.getContext().setInitSubindex(2);
        toolBar.setHidable(false);

        toolBar.addDockableBarListener(new DockableBarAdapter() {
            @Override
            public void dockableBarShown(DockableBarEvent event) {
                visatApp.updateState();
            }

            @Override
            public void dockableBarHidden(DockableBarEvent event) {
                visatApp.updateState();
            }
        });
       
        return toolBar;
    }

    public void addCommandsToToolBar(VisatApp myVisatApp, DockableBar toolBar, String commandID) {
        final Command command = myVisatApp.getCommandManager().getCommand(commandID);
        assert command != null : "command != null [commandID=".concat(commandID).concat("]");
        final AbstractButton toolBarButton = command.createToolBarButton();
        toolBarButton.addMouseListener(myVisatApp.getMouseOverActionHandler());
        toolBar.add(toolBarButton);
        toolBar.add(Box.createHorizontalStrut(1));
    }

    public void stop(VisatApp visatApp) {
    }

    /**
     * Tells a plug-in to update its component tree (if any) since the Java look-and-feel has changed.
     * <p/>
     * <p>If a plug-in uses top-level containers such as dialogs or frames, implementors of this method should invoke
     * <code>SwingUtilities.updateComponentTreeUI()</code> on such containers.
     */
    public void updateComponentTreeUI() {
    }

    // VisatPlugIn interface implementation
    /////////////////////////////////////////////////////////////////////////

    public static synchronized IasiFootprintVPI getInstance() {
        return instance;
    }

    public VisatApp getVisatApp() {
        return visatApp;
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

    public synchronized ProductSceneView getProductSceneView(InternalFrameEvent e) {
        final JInternalFrame internalFrame = e.getInternalFrame();
        final Container contentPane = internalFrame.getContentPane();
        if (contentPane instanceof ProductSceneView) {
            return (ProductSceneView) contentPane;
        }
        return null;
    }

    public static <T extends Layer> T getActiveFootprintLayer(Class<T> layerType) {
        final IasiFootprintVPI vpi = getInstance();
        final VisatApp app = vpi.getVisatApp();
        final ProductSceneView psv = app.getSelectedProductSceneView();
        Layer rootLayer = psv.getRootLayer();
        return vpi.getLayer(rootLayer, layerType);
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
        if(avhrrFileLocation == null) {
            // might be a subset - subsets are not yet supported
            return;
        }
        final String avhrrFilename = avhrrFileLocation.getName();
        final File avhrrDir = avhrrFileLocation.getParentFile();

        long avhrrStartTime = 0;
        try {
            avhrrStartTime = IasiFile.extractStartTimeInMillis(avhrrFilename);
        } catch (ParseException e) {
            // ignore
        }
        AvhrrProductInfo avhrrInfo = new AvhrrProductInfo(avhrrProduct, avhrrFilename, avhrrDir, avhrrStartTime);

        FilenameFilter iasiTimeFilter = new IasiFile.NameFilter(avhrrInfo.avhrrFilename);
        FilenameFilter amsuTimeFilter = new AmsuFile.NameFilter(avhrrInfo.avhrrFilename);
        FilenameFilter mhsTimeFilter = new MhsFile.NameFilter(avhrrInfo.avhrrFilename);
        
        addOverlayLayer(rootLayer, avhrrInfo, amsuTimeFilter, AMSU_NAME_FILTER, AmsuSounderLayer.class, amsuFootprintLayerModelMap, "AMSU");
        addOverlayLayer(rootLayer, avhrrInfo, mhsTimeFilter, MHS_NAME_FILTER, MhsSounderLayer.class, mhsFootprintLayerModelMap, "MHS");
        addOverlayLayer(rootLayer, avhrrInfo, iasiTimeFilter, IASI_NAME_FILTER, IasiFootprintLayer.class, iasiFootprintLayerModelMap, "IASI");
    }
    
    private <T extends Layer> T getLayer(Layer rootLayer, Class<T> layerType) {
        if (layerType.isAssignableFrom(rootLayer.getClass())) {
            return (T) rootLayer;
        }
        for (Layer childLayer : rootLayer.getChildren()) {
            Layer layer = getLayer(childLayer, layerType);
            if (layer != null) {
                return (T) layer;
            }
        }
        return null;
    }
    
    private <T extends Layer> boolean hasLayer(Layer layer, Class<T> layerType) {
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

    private void addOverlayLayer(Layer rootLayer, AvhrrProductInfo avhrrInfo, FilenameFilter timeFilter, FileFilter nameFilter, Class<? extends Layer> layerType, HashMap<Product, AvhrrOverlay> overlayMap, String type) {
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
                EpsFile epsFile = overlay.getEpsFile();
                Layer layer = epsFile.createLayer(overlay);
                rootLayer.getChildren().add(0, layer);
                // TODO (mz, 11,11,2008) HACK, because there is something wrong with the change listener
                layer.setVisible(false);
                layer.setVisible(true);
            }
        }
    }
    
    private EpsFile openEpsFile(AvhrrProductInfo avhrrInfo, FilenameFilter timeFilter, FileFilter nameFilter, String type) {
        File file = EpsFile.findFile(avhrrInfo.avhrrStartTime, avhrrInfo.avhrrDir.listFiles(timeFilter));
        if (file == null) {
            if (visatApp.showQuestionDialog(type +" Footprint Layer",
                                            "No matching "+type+" file was found for this AVHRR scene.\n" +
                                                    "Do you want to choose one manually?",
                                            FOOTPRINT_CHOOSE_OPTION_NAME) == JOptionPane.YES_OPTION) {
                file = showOpenFileDialog("Open "+type+" File", nameFilter, avhrrInfo.avhrrDir);
            }
        }
        if (file == null) {
            return null;
        }
        try {
            return EpsFormats.getInstance().openFile(file);
        } catch (IOException e) {
            visatApp.showErrorDialog(type+" Footprint Layer",
                                     "Not able to create "+type+" Footprint layer.");
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

    private synchronized void removeFootprintLayer(ProductSceneView psv) {
//        Layer rootLayer = psv.getRootLayer();
//        if (rootLayer.getChildren().contains(selectedLayer)) {
//            // todo warn
//            rootLayer.getChildren().remove(selectedLayer);
//        } else {
//            // todo warn
//        }
//        rootLayer.
//        final LayerModel layerModel = psv.getImageDisplay().getLayerModel();
//        final IasiFootprintLayer footprintLayer = getLayer(layerModel, IasiFootprintLayer.class);
//        if (footprintLayer != null) {
//            layerModel.removeLayer(footprintLayer);
//        }
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
                removeFootprintLayer(psv);
            }
        }

    }

    private class ProductTreeHandler implements ProductTreeListener {
        public void productAdded(Product product) {
        }

        public void productRemoved(Product avhrrProduct) {
//            SounderOverlay overlay = iasiFootprintLayerModelMap.get(avhrrProduct);
//            if (overlay != null) {
//                EpsFile epsFile = overlay.getEpsFile();
//                epsFile.close();
//                iasiFootprintLayerModelMap.remove(avhrrProduct);
//            }
            
            AvhrrOverlay overlay = amsuFootprintLayerModelMap.get(avhrrProduct);
            if (overlay != null) {
                EpsFile epsFile = overlay.getEpsFile();
                epsFile.close();
                amsuFootprintLayerModelMap.remove(avhrrProduct);
            }
            
            overlay = mhsFootprintLayerModelMap.get(avhrrProduct);
            if (overlay != null) {
                EpsFile epsFile = overlay.getEpsFile();
                epsFile.close();
                mhsFootprintLayerModelMap.remove(avhrrProduct);
            }
        }

        public void productSelected(Product product, int clickCount) {
        }

        public void metadataElementSelected(MetadataElement group, int clickCount) {
        }

        public void tiePointGridSelected(TiePointGrid tiePointGrid, int clickCount) {
        }

        public void bandSelected(Band band, int clickCount) {
        }
    }

}