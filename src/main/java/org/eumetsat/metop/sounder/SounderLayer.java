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
package org.eumetsat.metop.sounder;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.framework.ui.AbstractLayerUI;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.math.MathUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;

import javax.swing.SwingWorker;

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;


public class SounderLayer extends Layer {

    private final SounderOverlay overlay;
    private final SounderOverlayListener listener;
    
    private ColorInfo colorInfo;
    private boolean loadingColorInfo;
    
    private final BasicStroke borderStroke;
    private final Color ifovSelectedColor;

    private int height;
    private final int width;

    private BandInfo bandInfo;

    public SounderLayer(SounderOverlay overlay, int productWidth) throws IOException {
        this.overlay = overlay;
        
        borderStroke = new BasicStroke(0.0f);
        ifovSelectedColor = Color.GREEN;

        width = productWidth;
        height = overlay.getEpsFile().getMdrCount();
        
        listener = new OverlayListener();
        overlay.addListener(listener);
    }
    
    public BandInfo getBandInfo() {
        return bandInfo;
    }

    public synchronized void setBandInfo(BandInfo bandInfo) throws IOException {
        this.bandInfo = bandInfo;
        this.colorInfo = null;
    }

    public SounderOverlay getOverlay() {
        return overlay;
    }
    
    @Override
    public void renderLayer(Rendering rendering) {
        if (overlay.getIfovs().length == 0) {
            return;
        }
        ColorInfo cInfo = getColorInfo();
        if (cInfo == null) {
            return;
        }
        
        final Graphics2D g2d = rendering.getGraphics();
        final Viewport vp = rendering.getViewport();
        final AffineTransform transformSave = g2d.getTransform();
        try {
            final AffineTransform transform = new AffineTransform();
            transform.concatenate(transformSave);
            transform.concatenate(vp.getModelToViewTransform());
            g2d.setTransform(transform);
            
            final Color oldColor = g2d.getColor();
            final Paint oldPaint = g2d.getPaint();
            final Stroke oldStroke = g2d.getStroke();
            final Object oldAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            final Object oldRendering = g2d.getRenderingHint(RenderingHints.KEY_RENDERING);
            g2d.setStroke(borderStroke);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            final Rectangle clip = g2d.getClipBounds();
            
//            final double scale = Math.abs(vp.getModelToViewTransform().getDeterminant());
//            final boolean ifovBigEnough = scale * 47 > 5; // TODO

            final SounderIfov[] ifovs = overlay.getIfovs();
            final SounderIfov selectedIfov = overlay.getSelectedIfov();
//            if (ifovBigEnough) {
                for (SounderIfov ifov : ifovs) {
                    final Shape ifovShape = ifov.shape;
                    boolean isVisible = clip == null || ifovShape.intersects(clip);
                    if (isVisible) {
                        Color fillColor = cInfo.getColor(ifov.ifovInMdrIndex, ifov.mdrIndex);
                        g2d.setPaint(fillColor);
                        g2d.fill(ifovShape);
                        if (selectedIfov != null && selectedIfov == ifov) {
                            g2d.setColor(ifovSelectedColor);
                            g2d.draw(ifovShape);
                        }
                    }
                }
//            }

            g2d.setColor(oldColor);
            g2d.setPaint(oldPaint);
            g2d.setStroke(oldStroke);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, oldRendering);
        } finally {
            g2d.setTransform(transformSave);
        }
    }
    
    public SounderIfov getIfovForLocation(int pixelX, int pixelY) {
        SounderIfov[] ifovs = overlay.getIfovs();
        for (SounderIfov ifov : ifovs) {
            if (ifov.shape.contains(pixelX + 0.5f, pixelY + 0.5f)) {
                return ifov;
            }
        }
        return null;
    }
    
    private synchronized ColorInfo getColorInfo() {
        if (colorInfo != null) {
            return colorInfo;
        }
        if (loadingColorInfo) {
            return null;
        }
        loadingColorInfo = true;
        SwingWorker<ColorInfo, Object> worker = new SwingWorker<ColorInfo, Object>() {

            @Override
            protected ColorInfo doInBackground() throws Exception {
                ProductData buffer = ProductData.createInstance(bandInfo.getType(), width * height);
                overlay.getEpsFile().readData(bandInfo.getReader(), 0, 0, width, height, buffer , ProgressMonitor.NULL);
                return new ColorInfo(buffer);
            }
            
            @Override
            protected void done() {
                try {
                    colorInfo = get();
                    loadingColorInfo = false;
                    fireLayerDataChanged(null);
                } catch (Exception e) {
                    loadingColorInfo = false;
                    Debug.trace(e);
                }
            }
        }; 
        worker.execute();
        return null;
    }
    
    private class ColorInfo {
        private final ProductData buffer;
        private ColorPaletteDef paletteDef;
        private Color[] colorPalette;
        
        public ColorInfo(ProductData buffer) {
            this.buffer = buffer;
            computeColorPalette();
        }
        
        public Color getColor(int x, int y) {
            double sample = buffer.getElemDoubleAt(x + y * width);
            int numColors = colorPalette.length;
            double min = paletteDef.getMinDisplaySample();
            double max = paletteDef.getMaxDisplaySample();
            int index = MathUtils.floorAndCrop((sample - min) * (numColors - 1.0) / (max - min), 0, numColors-1);
            return colorPalette[index];
        }
        
        private void computeColorPalette() {
            int valueMin = Integer.MAX_VALUE;
            int valueMax = 0;
            for (int i = 0; i < buffer.getNumElems(); i++) {
                final int value = buffer.getElemIntAt(i);
                valueMin = Math.min(valueMin, value);
                valueMax = Math.max(valueMax, value);
            }
            paletteDef = new ColorPaletteDef(valueMin, valueMax);
            colorPalette = paletteDef.createColorPalette(Scaling.IDENTITY);
        }
    }
    
    private class OverlayListener implements SounderOverlayListener {

        @Override
        public void dataChanged(SounderOverlay overlay) {
            fireLayerDataChanged(null);
        }

        @Override
        public void selectionChanged(SounderOverlay overlay) {
            fireLayerDataChanged(null);
        }
    }
    
    private static class LayerUI extends AbstractLayerUI {

        protected LayerUI(Layer layer) {
            super(layer);
        }

        @Override
        public void handleSelection(ProductSceneView view, Rectangle rectangle) {
            SounderLayer selectedSounderLayer = (SounderLayer) getLayer();
            Point2D.Float point = new Point2D.Float(rectangle.x, rectangle.y);
            view.getLayerCanvas().getViewport().getViewToModelTransform().transform(point, point);
            SounderIfov ifov = selectedSounderLayer.getIfovForLocation(Math.round(point.x), Math.round(point.y));
            selectedSounderLayer.getOverlay().setSelectedIfov(ifov);
        }
    }

    public static class LayerUIFactory implements ExtensionFactory<SounderLayer> {

        @Override
        public <E> E getExtension(SounderLayer sounderLayer, Class<E> extensionType) {
            return (E) new LayerUI(sounderLayer);
        }

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[]{LayerUI.class};
        }
    }
}
