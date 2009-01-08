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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.iasi.visat.MetopSounderSupport;
import org.eumetsat.metop.amsu.AmsuAvhrrOverlay;
import org.eumetsat.metop.amsu.AmsuFile;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;


public class AddMetopOverlayAction extends ExecCommand {

    
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
//        registerHandler(); TODO re-enable
        Layer rootLayer = psv.getRootLayer();
        final Product avhrrProduct = psv.getProduct();
        AmsuAvhrrOverlay overlay = getOverlay(avhrrProduct);
        
        if (overlay != null && !hasLayer(rootLayer, overlay)) {
            MetopSounderLayer footprintLayer = new MetopSounderLayer(overlay);
            rootLayer.getChildren().add(0, footprintLayer);
            // TODO (mz, 11,11,2008) HACK, because there is something wrong with the change listener
            footprintLayer.setVisible(false);
            footprintLayer.setVisible(true);
        }
    }
    
    private boolean hasLayer(Layer layer, AmsuAvhrrOverlay overlay) {
        if (MetopSounderLayer.class.isAssignableFrom(layer.getClass()) &&
                ((MetopSounderLayer) layer).getOverlay() == overlay) {
            return true;
        }
        for (Layer childLayer : layer.getChildren()) {
            if (MetopSounderLayer.class.isAssignableFrom(childLayer.getClass()) &&
                    ((MetopSounderLayer) childLayer).getOverlay() == overlay) {
                return true;
            }
        }
        return false;
    }
    
    private AmsuAvhrrOverlay getOverlay(Product avhrrProduct) {
        ProductManager productManager = VisatApp.getApp().getProductManager();
        Product[] products = productManager.getProducts();
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        boolean nothingFound = true;
        for (Product product : products) {
            if (product.getProductType().equals(AmsuFile.PRODUCT_TYPE)) {
                nothingFound = false;
                DefaultMutableTreeNode productNode = new DefaultMutableTreeNode(product.getName());
                for (Band band : product.getBands()) {
                    productNode.add(new DefaultMutableTreeNode(band.getName()));
                }
                rootNode.add(productNode);
            }
        }
        if (nothingFound) {
            return null;
        }
        JTree tree = new JTree(rootNode);
        ModalDialog dialog = new ModalDialog(SwingUtilities.getWindowAncestor(VisatApp.getApp().getMainFrame()),
                                             "Select Overlay Band", new JScrollPane(tree), ModalDialog.ID_OK
                                                     + ModalDialog.ID_CANCEL, null);
        int result = dialog.show();
        TreePath selectionPath = tree.getSelectionPath();
        if (result == ModalDialog.ID_OK && selectionPath != null ) {
            String productName = (String) ((DefaultMutableTreeNode) selectionPath.getPathComponent(1)).getUserObject();
            String bandName = (String) ((DefaultMutableTreeNode) selectionPath.getPathComponent(2)).getUserObject();
            Product amsuProduct = productManager.getProduct(productName);
            return new AmsuAvhrrOverlay(avhrrProduct, amsuProduct, bandName);
        } else {
            return null;
        }
    }
}