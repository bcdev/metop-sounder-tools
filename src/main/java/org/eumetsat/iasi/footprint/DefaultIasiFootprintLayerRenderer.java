package org.eumetsat.iasi.footprint;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.util.math.MathUtils;
import org.eumetsat.metop.iasi.Efov;
import org.eumetsat.metop.iasi.IasiFile;
import org.eumetsat.metop.iasi.Ifov;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import com.bc.ceres.grender.Viewport;


public class DefaultIasiFootprintLayerRenderer implements IasiFootprintLayerRenderer {

    // Layer style properties
    private final BasicStroke borderStroke;
    private final Color efovColor;
    private final Color ifovSelectedColor;
    private final Color ifovNormalColor;
    private final Color ifovAnomalousColor;
    private double[][][] allBts;
    private ColorPaletteDef paletteDef;
    private Color[] colorPalette;

    public DefaultIasiFootprintLayerRenderer() {
        // Set default layer style properties
        this.borderStroke = new BasicStroke(0.0f);
        efovColor = Color.WHITE;
        ifovSelectedColor = Color.GREEN;
        ifovNormalColor = Color.RED;
        ifovAnomalousColor = Color.WHITE;
    }

    @Override
    public final void render(IasiFootprintLayer layer, Graphics2D g2d, Viewport viewport) {
        final IasiFootprintLayerModel layerModel = layer.getModel();
        final Color oldColor = g2d.getColor();
        final Paint oldPaint = g2d.getPaint();
        final Stroke oldStroke = g2d.getStroke();
        final Object oldAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        final Object oldRendering = g2d.getRenderingHint(RenderingHints.KEY_RENDERING);
        g2d.setStroke(borderStroke);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        final Rectangle clip = g2d.getClipBounds();

        final double scale = Math.abs(viewport.getModelToViewTransform().getDeterminant());
        final boolean efovBigEnough = scale * IasiFootprintLayer.EFOV_SIZE > 10;
        final boolean ifovBigEnough = scale * IasiFootprintLayer.IFOV_SIZE > 5;

        final Efov[] efovs = layerModel.getIasiAvhrrOverlay().getEfovs();
        
        if (allBts == null) {
            getData(layerModel);
        }

        if (efovBigEnough) {
            for (final Efov efov : efovs) {
                final Rectangle2D efovBounds = efov.getShape().getBounds2D();

                boolean efovVisible = clip == null || clip.intersects(efovBounds) || clip.contains(efovBounds);
                if (efovVisible) {
                    if (shouldRenderEfov(efov)) {
                        renderEfov(layerModel, g2d, efov);
                        if (ifovBigEnough) {
                            for (Ifov ifov : efov.getIfovs()) {
                                if (shouldRenderIfov(ifov)) {
                                    int mdrIndex = IasiFile.computeMdrIndex(ifov.getIndex());
                                    int efovIndex = IasiFile.computeEfovIndex(ifov.getIndex());
                                    int ifovIndex = IasiFile.computeIfovIndex(ifov.getIndex());
                                    renderIfov(layerModel, g2d, ifov, allBts[mdrIndex] [efovIndex][ifovIndex]);
                                }
                            }
                        }
                    }
                }
            }
        }

        g2d.setColor(oldColor);
        g2d.setPaint(oldPaint);
        g2d.setStroke(oldStroke);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, oldRendering);
    }
    
    private void getData(IasiFootprintLayerModel layerModel) {
        try {
            allBts = layerModel.getIasiAvhrrOverlay().getIasiFile().readAllBts(42);
        } catch (IOException e) {
            return;
        }
        double min = Double.MAX_VALUE;
        double max = 0;
        for (int i = 0; i < allBts.length; i++) {
            for (int j = 0; j < allBts[i].length; j++) {
                for (int k = 0; k < allBts[i][j].length; k++) {
                    min = Math.min(min, allBts[i][j][k]);
                    max = Math.max(min, allBts[i][j][k]);
                }
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

    protected boolean shouldRenderEfov(Efov efov) {
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

    protected boolean shouldRenderIfov(Ifov ifov) {
        return (!ifov.isAnomalous() || "true".equals(System.getProperty("iasi.renderAnomalousIfovs", "true")));
    }

    protected void renderEfov(IasiFootprintLayerModel layerModel, Graphics2D g2d, Efov efov) {
        g2d.setColor(efovColor);
        g2d.draw(efov.getShape());
    }

    // mz old code
//    protected void renderIfov(IasiFootprintLayerModel layerModel, Graphics2D g2d, Ifov ifov) {
//        final Shape ifovShape = ifov.getShape();
//        boolean selected = layerModel.isSelectedIfov(ifov);
//        g2d.setColor(selected ? ifovSelectedColor : ifov.isAnomalous() ? ifovAnomalousColor : ifovNormalColor);
//        g2d.draw(ifovShape);
//    }
    
    protected void renderIfov(IasiFootprintLayerModel layerModel, Graphics2D g2d, Ifov ifov, double bt) {
        final Shape ifovShape = ifov.getShape();
        boolean selected = layerModel.isSelectedIfov(ifov);
        g2d.setColor(selected ? ifovSelectedColor : ifov.isAnomalous() ? ifovAnomalousColor : ifovNormalColor);
        g2d.setPaint(getColor(bt));
        g2d.fill(ifovShape);
    }

    public void regenerate() {
        allBts = null;
    }

    public Color[] getColorPalette() {
        return colorPalette;
    }
    
    public ColorPaletteDef getColorPaletteDef() {
        return paletteDef;
    }
}
