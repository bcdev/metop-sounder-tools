package org.eumetsat.iasi.visat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.iasi.footprint.DefaultIasiFootprintLayerModel;
import org.eumetsat.iasi.footprint.DefaultIasiFootprintLayerRenderer;
import org.eumetsat.iasi.footprint.IasiFootprintLayer;
import org.eumetsat.iasi.footprint.IasiFootprintLayerModel;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.eps.EpsFormats;
import org.eumetsat.metop.iasi.IasiOverlay;
import org.eumetsat.metop.iasi.IasiFile;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.bc.ceres.glayer.Layer;

public class AddSounderOverlayAction extends ExecCommand {
    
    private static final String IASI_FOOTPRINT_CHOOSE_OPTION_NAME = "iasi.file.chooser";
    private Map<Product, Map<File, IasiFootprintLayerModel>> iasiFootprintLayerModelMap;
    private boolean handlerRegistered = false;
    
    
    public AddSounderOverlayAction() {
        iasiFootprintLayerModelMap = new WeakHashMap<Product, Map<File, IasiFootprintLayerModel>>(42);
    }
    
    @Override
    public void actionPerformed(CommandEvent event) {
        ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null && MetopSounderSupport.isValidAvhrrProductSceneView(sceneView)) {
            addIasiFootprintLayer(sceneView);
            sceneView.repaint();
        }
    }

    @Override
    public void updateState(CommandEvent event) {
        ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        boolean enabled = false;
        if (sceneView != null && MetopSounderSupport.isValidAvhrrProductSceneView(sceneView)) {
            enabled = true;
        }
        setEnabled(enabled);
    }
    
    private synchronized void addIasiFootprintLayer(ProductSceneView psv) {
        registerHandler();
        Layer rootLayer = psv.getRootLayer();
        final Product avhrrProduct = psv.getProduct();
        IasiFootprintLayerModel layerModel = getIasiFootprintLayerModel(avhrrProduct);
        
        if (layerModel != null && !hasLayer(rootLayer, layerModel)) {
            IasiFootprintLayer footprintLayer = new IasiFootprintLayer(layerModel, new DefaultIasiFootprintLayerRenderer());
            rootLayer.getChildren().add(0, footprintLayer);
            // TODO (mz, 11,11,2008) HACK, because there is something wrong with the change listener
            footprintLayer.setVisible(false);
            footprintLayer.setVisible(true);
        }
    }
    
    private boolean hasLayer(Layer layer, IasiFootprintLayerModel model) {
        if (IasiFootprintLayer.class.isAssignableFrom(layer.getClass()) &&
                ((IasiFootprintLayer) layer).getModel() == model) {
            return true;
        }
        for (Layer childLayer : layer.getChildren()) {
            if (IasiFootprintLayer.class.isAssignableFrom(childLayer.getClass()) &&
                    ((IasiFootprintLayer) childLayer).getModel() == model) {
                return true;
            }
        }
        return false;
    }
    
    private void registerHandler() {
        if (!handlerRegistered) {
            VisatApp.getApp().addProductTreeListener(new ProductTreeHandler());
            handlerRegistered = true;
        }
    }
    
    private File getSounderFileAutomatic(Product avhrrProduct) {
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
//            if (VisatApp.getApp().showQuestionDialog("IASI Footprint Layer",
//                                            "No matching IASI file was found for this AVHRR scene.\n" +
//                                                    "Do you want to choose one manually?",
//                                            IASI_FOOTPRINT_CHOOSE_OPTION_NAME) == JOptionPane.YES_OPTION) {
                file = showOpenFileDialog("Open IASI File", new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().matches("IASI_.*.nat");
                    }

                    @Override
                    public String getDescription() {
                        return "IASI Files (*.nat)";
                    }
                }, avhrrDir);
//            }
        }
        return file;
    }

    private IasiFootprintLayerModel getIasiFootprintLayerModel(Product avhrrProduct) {
        File file = getSounderFileAutomatic(avhrrProduct);
        if (file == null) {
            return null;
        }
        Map<File, IasiFootprintLayerModel> modelMap = iasiFootprintLayerModelMap.get(avhrrProduct);
        if (modelMap == null) {
            modelMap = new HashMap<File, IasiFootprintLayerModel>(5);
            iasiFootprintLayerModelMap.put(avhrrProduct, modelMap);
        } else {
            IasiFootprintLayerModel iasiFootprintLayerModel = modelMap.get(file);
            if (iasiFootprintLayerModel != null) {
                return iasiFootprintLayerModel;
            }
        }
        final IasiOverlay overlay;
        try {
            IasiFile iasiFile = (IasiFile) EpsFormats.getInstance().openFile(file);
            overlay = new IasiOverlay(iasiFile, avhrrProduct);
            
//            final int avhrrRasterHeight = avhrrProduct.getSceneRasterHeight();
//            final long avhrrEndMillis = avhrrProduct.getEndTime().getAsCalendar().getTimeInMillis();
//            final long avhrrStartMillis = avhrrProduct.getStartTime().getAsCalendar().getTimeInMillis();
//            final int avhrrTrimLeft = avhrrProduct.getMetadataRoot().getElement("READER_INFO").getAttributeInt("TRIM_LEFT", 0);
//            iasiFile = new IasiFile(file, avhrrStartMillis, avhrrEndMillis, avhrrRasterHeight,
//                                    avhrrTrimLeft);
        } catch (IOException e) {
            VisatApp.getApp().showErrorDialog("IASI Footprint Layer",
                                     "Not able to create IASI Footprint layer.");
            return null;
        }
        IasiFootprintLayerModel newLayerModel = new DefaultIasiFootprintLayerModel(overlay);
        modelMap.put(file, newLayerModel);
        return newLayerModel;
    }
    
    private File showOpenFileDialog(String title, FileFilter fileFilter, File currentDir) {
        BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        if (fileFilter != null) {
            fileChooser.setFileFilter(fileFilter);
        }
        VisatApp visatApp = VisatApp.getApp();
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

    private class ProductTreeHandler implements ProductTreeListener {
        public void productAdded(Product product) {
        }

        public void productRemoved(Product avhrrProduct) {
            Map<File, IasiFootprintLayerModel> modelMap = iasiFootprintLayerModelMap.get(avhrrProduct);
            if (modelMap != null) {
                for (IasiFootprintLayerModel layerModel : modelMap.values()) {
                    layerModel.dispose();
                }
            }
            iasiFootprintLayerModelMap.remove(avhrrProduct);
            Debug.trace("IasiFootprintVPI: Removed association of IasiFootprintLayerModel with product " + avhrrProduct.getName());
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
