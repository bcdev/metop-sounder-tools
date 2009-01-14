/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.iasi.visat.MetopSounderSupport;
import org.eumetsat.metop.amsu.AmsuSounderLayer;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.eps.EpsReader;
import org.eumetsat.metop.sounder.SounderLayer;
import org.eumetsat.metop.sounder.SounderOverlay;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;


public class AddMetopOverlayAction extends ExecCommand {

    private Map<Product, Map<EpsFile, SounderOverlay>> overlayMap;
    private boolean handlerRegistered = false;
    
    public AddMetopOverlayAction() {
        overlayMap = new WeakHashMap<Product, Map<EpsFile,SounderOverlay>>(8);
    }
    
    @Override
    public void actionPerformed(CommandEvent event) {
        ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null && MetopSounderSupport.isValidAvhrrProductSceneView(sceneView)) {
            addFootprintLayer(sceneView);
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
    
    private synchronized void addFootprintLayer(ProductSceneView psv) {
        registerHandler();
        Layer rootLayer = psv.getRootLayer();
        final Product avhrrProduct = psv.getProduct();
        EpsFile selectedEpsFile = selectOverlayProduct(avhrrProduct);
        SounderOverlay overlay = selectedEpsFile.createOverlay(avhrrProduct);
        
        if (overlay != null && !hasLayer(rootLayer, overlay)) {
            Layer layer = selectedEpsFile.createLayer(overlay);
            rootLayer.getChildren().add(0, layer);
            // TODO (mz, 11,11,2008) HACK, because there is something wrong with the change listener
            layer.setVisible(false);
            layer.setVisible(true);
        }
    }
    
    private void registerHandler() {
        if (!handlerRegistered) {
            VisatApp.getApp().addProductTreeListener(new ProductTreeHandler());
            handlerRegistered = true;
        }
    }
    
    private boolean hasLayer(Layer layer, SounderOverlay overlay) {
        if (SounderLayer.class.isAssignableFrom(layer.getClass()) &&
                ((SounderLayer) layer).getOverlay() == overlay) {
            return true;
        }
        for (Layer childLayer : layer.getChildren()) {
            if (SounderLayer.class.isAssignableFrom(childLayer.getClass()) &&
                    ((SounderLayer) childLayer).getOverlay() == overlay) {
                return true;
            }
        }
        return false;
    }
    
    private EpsFile selectOverlayProduct(Product avhrrProduct) {
        ProductManager productManager = VisatApp.getApp().getProductManager();
        Product[] products = productManager.getProducts();
        DefaultListModel listModel = new DefaultListModel();
        for (Product product : products) {
            ProductReader productReader = product.getProductReader();
            if (productReader instanceof EpsReader) {
                EpsReader epsReader = (EpsReader) productReader;
                EpsFile epsFile = epsReader.getEpsFile();
                if (epsFile.hasOverlayFor(avhrrProduct)) {
                    listModel.addElement(product.getName());
                }
            }
        }
        if (listModel.capacity() == 0) {
            return null;
        }
        JList list = new JList(listModel);
        ModalDialog dialog = new ModalDialog(SwingUtilities.getWindowAncestor(VisatApp.getApp().getMainFrame()),
                                             "Select Overlay Product", new JScrollPane(list), ModalDialog.ID_OK
                                                     + ModalDialog.ID_CANCEL, null);
        int result = dialog.show();
        int selectedIndex = list.getSelectedIndex();
        if (result == ModalDialog.ID_OK && selectedIndex != -1) {
            String selecteProductName = (String) listModel.getElementAt(selectedIndex);
            Product amsuProduct = productManager.getProduct(selecteProductName);
            ProductReader productReader = amsuProduct.getProductReader();
            if (productReader instanceof EpsReader) {
                EpsReader epsReader = (EpsReader) productReader;
                return epsReader.getEpsFile();
            }
        }
        return null;
    }
    
    private SounderOverlay getOverlay(EpsFile epsFile, Product avhrrProduct) {
        synchronized (overlayMap) {
            if (!overlayMap.containsKey(avhrrProduct)) {
                HashMap<EpsFile, SounderOverlay> hashMap = new HashMap<EpsFile, SounderOverlay>();
                overlayMap.put(avhrrProduct, hashMap);
            }
            Map<EpsFile, SounderOverlay> overlays = overlayMap.get(avhrrProduct);
            if (!overlays.containsKey(epsFile)) {
                SounderOverlay avhrrOverlay = epsFile.createOverlay(avhrrProduct);
                overlays.put(epsFile, avhrrOverlay);
            }
            return overlays.get(epsFile);
        }
    }
    
    private class ProductTreeHandler implements ProductTreeListener {
        public void productAdded(Product product) {
        }

        public void productRemoved(Product avhrrProduct) {
            overlayMap.remove(avhrrProduct);
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
