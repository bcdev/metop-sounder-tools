/*
 * $Id: SampleRecordsLayer.java,v 1.5 2005/08/16 03:58:04 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.eumetsat.metop.iasi;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
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
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import javax.swing.SwingWorker;

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

public class IasiLayer extends Layer {

    public static final int EFOV_SIZE = 50;
    public static final int IFOV_SIZE = 12;
    public static final float IFOV_DIST = 18;
    
    // Layer style properties
    private final BasicStroke borderStroke;
    private final Color efovColor;
    private final Color ifovSelectedColor;
    private final Color ifovAnomalousColor;

    private ColorInfo colorInfo;
    private boolean loadingColorInfo;
    
    private final OverlayListener overlayListener;
    private final IasiOverlay iasiOverlay;

    public IasiLayer(IasiOverlay iasiOverlay) {
        this.iasiOverlay = iasiOverlay;
        
        overlayListener = new OverlayListener();
        iasiOverlay.addListener(overlayListener);
      
        // Set default layer style properties
        this.borderStroke = new BasicStroke(0.4f);
        efovColor = Color.WHITE;
        ifovSelectedColor = Color.GREEN;
        //ifovNormalColor = Color.RED;
        ifovAnomalousColor = Color.RED;
    }
    
    public IasiOverlay getOverlay() {
        return iasiOverlay;
    }

    @Override
    public String getName() {
        try {
            return "IASI L1C ("+iasiOverlay.getEpsFile().getProductName()+")";
        } catch (IOException e) {
            return "";
        }
    }

    private IasiIfov getIfovForLocation(int pixelX, int pixelY) {
        Efov[] efovs = iasiOverlay.getEfovs();
        for (final Efov efov : efovs) {
            for (final IasiIfov ifov : efov.getIfovs()) {
                final boolean renderAnomalousIfovs = "true".equals(System.getProperty("iasi.renderAnomalousIfovs", "true"));
                if (!ifov.isAnomalous() || renderAnomalousIfovs) {
                    if (ifov.getShape().contains(pixelX + 0.5f, pixelY + 0.5f)) {
                        return ifov;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void renderLayer(Rendering rendering) {
        Efov[] efovs = iasiOverlay.getEfovs();
        if (efovs.length == 0) {
            return;
        }
        ColorInfo cInfo = getColorInfo();
        if (cInfo == null) {
            return;
        }
        
        final Graphics2D g2d = rendering.getGraphics();
        final Viewport vp = rendering.getViewport();
        final AffineTransform transformSave = g2d.getTransform();
        
        final AffineTransform transform = new AffineTransform();
        transform.concatenate(transformSave);
        transform.concatenate(vp.getModelToViewTransform());
        g2d.setTransform(transform);
        
        final Color oldColor = g2d.getColor();
        final Paint oldPaint = g2d.getPaint();
        final Stroke oldStroke = g2d.getStroke();
        final Object oldAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        final Object oldRendering = g2d.getRenderingHint(RenderingHints.KEY_RENDERING);
        try {
            g2d.setStroke(borderStroke);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            final Rectangle clip = g2d.getClipBounds();

            final double scale = Math.abs(vp.getModelToViewTransform().getDeterminant());
            final boolean efovBigEnough = scale * IasiLayer.EFOV_SIZE > 10;
            final boolean ifovBigEnough = scale * IasiLayer.IFOV_SIZE > 5;

            if (efovBigEnough) {
                for (final Efov efov : efovs) {
                    final Rectangle2D efovBounds = efov.getShape().getBounds2D();

                    boolean efovVisible = clip == null || clip.intersects(efovBounds) || clip.contains(efovBounds);
                    if (efovVisible) {
                        if (shouldRenderEfov(efov)) {
                            renderEfov(g2d, efov);
                            if (ifovBigEnough) {
                                for (IasiIfov ifov : efov.getIfovs()) {
                                    if (shouldRenderIfov(ifov)) {
                                        int mdrIndex = IasiFile.computeMdrIndex(ifov.getIfovIndex());
                                        int efovIndex = IasiFile.computeEfovIndex(ifov.getIfovIndex());
                                        int ifovIndex = IasiFile.computeIfovIndex(ifov.getIfovIndex());
                                        renderIfov(g2d, ifov, cInfo.getColor(mdrIndex, efovIndex, ifovIndex));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            
            
        } finally {
            g2d.setColor(oldColor);
            g2d.setPaint(oldPaint);
            g2d.setStroke(oldStroke);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, oldRendering);
            g2d.setTransform(transformSave);
        }
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
                double[][][] allBts = iasiOverlay.getIasiFile().readAllBts(42);
                final Efov[] efovs = iasiOverlay.getEfovs();
                double min = Double.MAX_VALUE;
                double max = 0;
                int efovIndex = 0;
                for (int i = 0; i < allBts.length; i++) {
                    for (int j = 0; j < allBts[i].length; j++) {
                        Efov efov = efovs[efovIndex];
                        for (int k = 0; k < allBts[i][j].length; k++) {
                            if (!efov.getIfovs()[k].isAnomalous()) {
                                min = Math.min(min, allBts[i][j][k]);
                                max = Math.max(min, allBts[i][j][k]);
                            }
                        }
                        efovIndex++;
                    }
                }
                ColorPaletteDef paletteDef = new ColorPaletteDef(min, max);
                Color[] colorPalette = paletteDef.createColorPalette(Scaling.IDENTITY);
                return new ColorInfo(allBts, paletteDef, colorPalette);
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

    private boolean shouldRenderEfov(Efov efov) {
        final boolean renderAnomalousIfovs = "true".equals(System.getProperty("iasi.renderAnomalousIfovs", "true"));

        if (renderAnomalousIfovs) {
            return true;
        }
        for (final IasiIfov ifov : efov.getIfovs()) {
            if (!ifov.isAnomalous()) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldRenderIfov(IasiIfov ifov) {
        return (!ifov.isAnomalous() || "true".equals(System.getProperty("iasi.renderAnomalousIfovs", "true")));
    }

    private void renderEfov(Graphics2D g2d, Efov efov) {
        g2d.setColor(efovColor);
        g2d.draw(efov.getShape());
    }

    private void renderIfov(Graphics2D g2d, IasiIfov ifov, Color color) {
        final Shape ifovShape = ifov.getShape();
        if (!ifov.isAnomalous()) {
            g2d.setPaint(color);
            g2d.fill(ifovShape);
        }
        
        Color drawColor = null;
        if (iasiOverlay.getSelectedIfov() ==ifov) {
            drawColor = ifovSelectedColor;
        } else if (ifov.isAnomalous()) {
            drawColor = ifovAnomalousColor;
        }
        if (drawColor != null) {
            g2d.setColor(drawColor);
            g2d.draw(ifovShape);
        }
    }    
    
    @Override
    public void regenerate() {
        colorInfo = null;
    }

    public Color[] getColorPalette() {
        return colorInfo.colorPalette;
    }
    
    public ColorPaletteDef getColorPaletteDef() {
        return colorInfo.paletteDef;
    }
    
    private class ColorInfo {
        private final double[][][] allBts;
        private ColorPaletteDef paletteDef;
        private Color[] colorPalette;
        
        public ColorInfo(double[][][] allBts, ColorPaletteDef paletteDef, Color[] colorPalette) {
            this.allBts = allBts;
            this.paletteDef = paletteDef;
            this.colorPalette = colorPalette;
        }
        
        public Color getColor(int mdrIndex, int efovIndex, int ifovIndex) {
            double sample = allBts[mdrIndex][efovIndex][ifovIndex];
            int numColors = colorPalette.length;
            double min = paletteDef.getMinDisplaySample();
            double max = paletteDef.getMaxDisplaySample();
            int index = MathUtils.floorAndCrop((sample - min) * (numColors - 1.0) / (max - min), 0, numColors-1);
            return colorPalette[index];
        }
    }

    private static class LayerUI extends AbstractLayerUI {

        protected LayerUI(Layer layer) {
            super(layer);
        }

        @Override
        public void handleSelection(ProductSceneView view, Rectangle rectangle) {
            IasiLayer selectedIasiLayer = (IasiLayer) getLayer();
            Point2D.Float point = new Point2D.Float(rectangle.x, rectangle.y);
            view.getLayerCanvas().getViewport().getViewToModelTransform().transform(point, point);
            IasiIfov ifov = selectedIasiLayer.getIfovForLocation(Math.round(point.x), Math.round(point.y));
            selectedIasiLayer.getOverlay().setSelectedIfov(ifov);
        }
    }

    public static class LayerUIFactory implements ExtensionFactory<IasiLayer> {

        @Override
        public <E> E getExtension(IasiLayer iasiLayer, Class<E> extensionType) {
            return (E) new LayerUI(iasiLayer);
        }

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[]{LayerUI.class};
        }
    }

    private class OverlayListener implements IasiOverlayListener {
        @Override
        public void selectionChanged(IasiOverlay overlay) {
            fireLayerDataChanged(null);
        }

        @Override
        public void dataChanged(IasiOverlay overlay) {
            fireLayerDataChanged(null);
        }
    }
}