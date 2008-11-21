/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package org.eumetsat.iasi.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.iasi.footprint.IasiFootprintLayer;

import java.awt.Container;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameEvent;

import com.bc.ceres.glayer.Layer;


public class MetopSounderSupport {

    private static final String[] METOP_AVHRR_SIGNATURE = {
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
    
    public static ProductSceneView getProductSceneView(InternalFrameEvent e) {
        final JInternalFrame internalFrame = e.getInternalFrame();
        final Container contentPane = internalFrame.getContentPane();
        if (contentPane instanceof ProductSceneView) {
            return (ProductSceneView) contentPane;
        }
        return null;
    }
    
    public static IasiFootprintLayer getActiveFootprintLayer() {
        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        Layer rootLayer = sceneView.getRootLayer();
        return getLayer(rootLayer, IasiFootprintLayer.class);
    }

    @SuppressWarnings({"unchecked"})
    public static <T extends Layer> T getLayer(final Layer layer, Class<T> layerType) {
        synchronized (layer) {
            if (layerType.isAssignableFrom(layer.getClass())) {
                return (T) layer;
            }
            for (Layer childLayer : layer.getChildren()) {
                if (layerType.isAssignableFrom(childLayer.getClass())) {
                    return (T) childLayer;
                }
            }
            return null;
        }
    }

}
