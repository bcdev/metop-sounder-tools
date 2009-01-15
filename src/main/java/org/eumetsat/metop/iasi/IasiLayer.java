/*
 * $Id: SampleRecordsLayer.java,v 1.5 2005/08/16 03:58:04 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.eumetsat.metop.iasi;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
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
import java.awt.geom.Rectangle2D;
import java.io.IOException;

public class IasiLayer extends Layer {

    public static final int EFOV_SIZE = 50;
    public static final int IFOV_SIZE = 12;
    public static final float IFOV_DIST = 18;
    
    // Layer style properties
    private final BasicStroke borderStroke;
    private final Color efovColor;
    private final Color ifovSelectedColor;
    private final Color ifovAnomalousColor;

    private double[][][] allBts;
    private ColorPaletteDef paletteDef;
    private Color[] colorPalette;

    private final LayerModelHandler modelListener;
    private final IasiOverlay iasiOverlay;

    public IasiLayer(IasiOverlay iasiOverlay) {
        this.iasiOverlay = iasiOverlay;
        
        // TODO - re-enable selection (mz, 07.11.2009)
        //setPropertyValue("selectTool", new IfovSelectTool());
        modelListener = new LayerModelHandler();
        iasiOverlay.addListener(modelListener);
      
        // Set default layer style properties
        this.borderStroke = new BasicStroke(0.0f);
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

    public Efov[] getEfovs() {
        return iasiOverlay.getEfovs();
    }

    public Ifov getIfovForLocation(int pixelX, int pixelY) {
        for (final Efov efov : getEfovs()) {
            for (final Ifov ifov : efov.getIfovs()) {
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
        if (getEfovs().length == 0) {
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

            final Efov[] efovs = iasiOverlay.getEfovs();
            
            ensureDataLoaded();

            if (efovBigEnough) {
                for (final Efov efov : efovs) {
                    final Rectangle2D efovBounds = efov.getShape().getBounds2D();

                    boolean efovVisible = clip == null || clip.intersects(efovBounds) || clip.contains(efovBounds);
                    if (efovVisible) {
                        if (shouldRenderEfov(efov)) {
                            renderEfov(g2d, efov);
                            if (ifovBigEnough) {
                                for (Ifov ifov : efov.getIfovs()) {
                                    if (shouldRenderIfov(ifov)) {
                                        int mdrIndex = IasiFile.computeMdrIndex(ifov.getIndex());
                                        int efovIndex = IasiFile.computeEfovIndex(ifov.getIndex());
                                        int ifovIndex = IasiFile.computeIfovIndex(ifov.getIndex());
                                        renderIfov(g2d, ifov, allBts[mdrIndex] [efovIndex][ifovIndex]);
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
    
    private synchronized void ensureDataLoaded() {
        try {
            allBts = iasiOverlay.getIasiFile().readAllBts(42);
        } catch (IOException e) {
            return;
        }
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
        paletteDef = new ColorPaletteDef(min, max);
        colorPalette = paletteDef.createColorPalette(Scaling.IDENTITY);
    }

    private Color getColor(double sample) {
        int numColors = colorPalette.length;
        double min = paletteDef.getMinDisplaySample();
        double max = paletteDef.getMaxDisplaySample();
        int index = MathUtils.floorAndCrop((sample - min) * (numColors - 1.0) / (max - min), 0, numColors-1);
        return colorPalette[index];
    }

    private boolean shouldRenderEfov(Efov efov) {
        final boolean renderAnomalousIfovs = "true".equals(System.getProperty("iasi.renderAnomalousIfovs", "true"));

        if (renderAnomalousIfovs) {
            return true;
        }
        for (final Ifov ifov : efov.getIfovs()) {
            if (!ifov.isAnomalous()) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldRenderIfov(Ifov ifov) {
        return (!ifov.isAnomalous() || "true".equals(System.getProperty("iasi.renderAnomalousIfovs", "true")));
    }

    private void renderEfov(Graphics2D g2d, Efov efov) {
        g2d.setColor(efovColor);
        g2d.draw(efov.getShape());
    }

    private void renderIfov(Graphics2D g2d, Ifov ifov, double bt) {
        final Shape ifovShape = ifov.getShape();
        if (!ifov.isAnomalous()) {
            g2d.setPaint(getColor(bt));
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
        allBts = null;
    }

    public Color[] getColorPalette() {
        return colorPalette;
    }
    
    public ColorPaletteDef getColorPaletteDef() {
        return paletteDef;
    }

    private class IfovSelectTool extends AbstractTool {

        @Override
        public void mousePressed(ToolInputEvent event) {
            final Ifov clickedIfov = getIfovForLocation(event.getPixelX(), event.getPixelY());
            if (clickedIfov != null) {
                iasiOverlay.setSelectedIfov(clickedIfov);
            }
        }
    }

    private class LayerModelHandler implements IasiOverlayListener {
        public void selectionChanged(IasiOverlay overlay) {
            fireLayerDataChanged(null);
        }
    }
}