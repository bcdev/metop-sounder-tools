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
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

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

    private static IasiFootprintVPI instance;
    private VisatApp visatApp;
    private static final String IASI_FOOTPRINT_CHOOSE_OPTION_NAME = "iasi.file.chooser";
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
//        final Command command = myVisatApp.getCommandManager().getCommand(commandID);
//        assert command != null : "command != null [commandID=".concat(commandID).concat("]");
//        final AbstractButton toolBarButton = command.createToolBarButton();
//        toolBarButton.addMouseListener(myVisatApp.getMouseOverActionHandler());
//        toolBar.add(toolBarButton);
//        toolBar.add(Box.createHorizontalStrut(1));
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

    public boolean isValidAvhrrProductSceneView(final ProductSceneView view) {
        return view != null && isValidAvhrrProduct(view.getProduct());
    }

    public boolean isValidAvhrrProduct(final Product product) {
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


//    public IasiFootprintLayer getActiveFootprintLayer() {
//        final IasiFootprintVPI vpi = getInstance();
//        final VisatApp app = vpi.getVisatApp();
//        final ProductSceneView sceneView = app.getSelectedProductSceneView();
//        return getFootprintLayer(sceneView);
//    }

//    public IasiFootprintLayer getFootprintLayer(ProductSceneView sceneView) {
//        final LayerModel layerModel = sceneView.getImageDisplay().getLayerModel();
//        return getLayer(layerModel, IasiFootprintLayer.class);
//    }

    /////////////////////////////////////////////////////////////////////////
    // Private stuff

    private synchronized void registerProductTreeListener() {
        visatApp.addProductTreeListener(new ProductTreeHandler());
    }

    private synchronized void registerInternalFrameHandler() {
        visatApp.addInternalFrameListener(new ProductSceneViewHook());
    }

    private synchronized void addFootprintLayer(ProductSceneView psv) {
        Layer rootLayer = psv.getRootLayer();
        //IASI
        if (!hasLayer(rootLayer, IasiFootprintLayer.class)) {
            final Product avhrrProduct = psv.getProduct();
            AvhrrOverlay overlay = iasiFootprintLayerModelMap.get(avhrrProduct);
            if (overlay == null) { 
                EpsFile epsFile = createIasiFootprintLayerModel(avhrrProduct);
                if (epsFile != null) {
                    overlay = epsFile.createOverlay(avhrrProduct);
                    iasiFootprintLayerModelMap.put(avhrrProduct, overlay);
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
        
        //AMSU
        if (!hasLayer(rootLayer, AmsuSounderLayer.class)) {
            final Product avhrrProduct = psv.getProduct();
            AvhrrOverlay overlay = amsuFootprintLayerModelMap.get(avhrrProduct);
            if (overlay == null) { 
                EpsFile epsFile = createAmsuFootprintLayerModel(avhrrProduct);
                if (epsFile != null) {
                    overlay = epsFile.createOverlay(avhrrProduct);
                    amsuFootprintLayerModelMap.put(avhrrProduct, overlay);
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
        
        //MHS
        if (!hasLayer(rootLayer, MhsSounderLayer.class)) {
            final Product avhrrProduct = psv.getProduct();
            AvhrrOverlay overlay = mhsFootprintLayerModelMap.get(avhrrProduct);
            if (overlay == null) { 
                EpsFile epsFile = createMhsFootprintLayerModel(avhrrProduct);
                if (epsFile != null) {
                    overlay = epsFile.createOverlay(avhrrProduct);
                    mhsFootprintLayerModelMap.put(avhrrProduct, overlay);
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
    
    private <T extends Layer> boolean hasLayer(Layer layer, Class<T> layerType) {
        if (layerType.isAssignableFrom(layer.getClass())) {
            return true;
        }
        for (Layer childLayer : layer.getChildren()) {
            if (layerType.isAssignableFrom(childLayer.getClass())) {
                return true;
            }
        }
        return false;
    }

    private EpsFile createIasiFootprintLayerModel(Product avhrrProduct) {
        final File avhrrFileLocation = avhrrProduct.getFileLocation();
        if(avhrrFileLocation == null) {
            // might be a subset - subsets are not yet supported
            return null;
        }
        final String avhrrFilename = avhrrFileLocation.getName();
        final File avhrrDir = avhrrFileLocation.getParentFile();

        File file = null;
        try {
            final long avhrrStartTime = IasiFile.extractStartTimeInMillis(avhrrFilename);
            file = EpsFile.findFile(avhrrStartTime, avhrrDir.listFiles(new IasiFile.IasiFilenameFilter(avhrrFilename)));
        } catch (ParseException e) {
            // ignore
        }
        if (file == null) {
            if (visatApp.showQuestionDialog("IASI Footprint Layer",
                                            "No matching IASI file was found for this AVHRR scene.\n" +
                                                    "Do you want to choose one manually?",
                                            IASI_FOOTPRINT_CHOOSE_OPTION_NAME) == JOptionPane.YES_OPTION) {
                file = showOpenFileDialog("Open IASI File", new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.getName().matches("IASI_.*.nat");
                    }

                    @Override
                    public String getDescription() {
                        return "IASI Files (*.nat)";
                    }
                }, avhrrDir);
            }
        }
        if (file == null) {
            return null;
        }
        try {
            return new IasiFile(file, EpsFormats.getInstance().getIasiDataFormat());
        } catch (IOException e) {
            visatApp.showErrorDialog("IASI Footprint Layer",
                                     "Not able to create IASI Footprint layer.");
            return null;
        }
    }
    
    private EpsFile createAmsuFootprintLayerModel(Product avhrrProduct) {
        final File avhrrFileLocation = avhrrProduct.getFileLocation();
        if(avhrrFileLocation == null) {
            // might be a subset - subsets are not yet supported
            return null;
        }
        final String avhrrFilename = avhrrFileLocation.getName();
        final File avhrrDir = avhrrFileLocation.getParentFile();

        File file = null;
        try {
            final long avhrrStartTime = IasiFile.extractStartTimeInMillis(avhrrFilename);
            file = EpsFile.findFile(avhrrStartTime, avhrrDir.listFiles(new AmsuFile.AmsuFilenameFilter(avhrrFilename)));
        } catch (ParseException e) {
            // ignore
        }
        if (file == null) {
            if (visatApp.showQuestionDialog("AMSU Footprint Layer",
                                            "No matching AMSU file was found for this AVHRR scene.\n" +
                                                    "Do you want to choose one manually?",
                                            IASI_FOOTPRINT_CHOOSE_OPTION_NAME) == JOptionPane.YES_OPTION) {
                file = showOpenFileDialog("Open AMSA File", new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.getName().matches("AMSA_.*.nat");
                    }

                    @Override
                    public String getDescription() {
                        return "AMSA Files (*.nat)";
                    }
                }, avhrrDir);
            }
        }
        if (file == null) {
            return null;
        }
        try {
            return EpsFormats.getInstance().openFile(file);
//            return new IasiFile(file, EpsFormats.getInstance().getIasiDataFormat());
        } catch (IOException e) {
            visatApp.showErrorDialog("AMSU Footprint Layer",
                                     "Not able to create AMSU Footprint layer.");
            return null;
        }
    }

    
    private EpsFile createMhsFootprintLayerModel(Product avhrrProduct) {
        final File avhrrFileLocation = avhrrProduct.getFileLocation();
        if(avhrrFileLocation == null) {
            // might be a subset - subsets are not yet supported
            return null;
        }
        final String avhrrFilename = avhrrFileLocation.getName();
        final File avhrrDir = avhrrFileLocation.getParentFile();

        File file = null;
        try {
            final long avhrrStartTime = IasiFile.extractStartTimeInMillis(avhrrFilename);
            file = EpsFile.findFile(avhrrStartTime, avhrrDir.listFiles(new MhsFile.MhsFilenameFilter(avhrrFilename)));
        } catch (ParseException e) {
            // ignore
        }
        if (file == null) {
            if (visatApp.showQuestionDialog("MHS Footprint Layer",
                                            "No matching MHS file was found for this AVHRR scene.\n" +
                                                    "Do you want to choose one manually?",
                                            IASI_FOOTPRINT_CHOOSE_OPTION_NAME) == JOptionPane.YES_OPTION) {
                file = showOpenFileDialog("Open MHS File", new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.getName().matches("MHSx_.*.nat");
                    }

                    @Override
                    public String getDescription() {
                        return "MHS Files (*.nat)";
                    }
                }, avhrrDir);
            }
        }
        if (file == null) {
            return null;
        }
        try {
            return EpsFormats.getInstance().openFile(file);
//            new IasiFile(file, EpsFormats.getInstance().getIasiDataFormat());
        } catch (IOException e) {
            visatApp.showErrorDialog("IASI Footprint Layer",
                                     "Not able to create IASI Footprint layer.");
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

    private class ProductSceneViewHook extends InternalFrameAdapter {
        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            final ProductSceneView psv = getProductSceneView(e);
            if (isValidAvhrrProductSceneView(psv)) {
                addFootprintLayer(psv);
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