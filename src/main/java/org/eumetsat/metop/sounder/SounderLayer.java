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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.util.math.MathUtils;
import org.eumetsat.metop.amsu.AmsuSounderOverlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.io.IOException;


public class SounderLayer extends Layer {

    private final SounderOverlay overlay;
    
    private Color[] colorPalette;
    private ColorPaletteDef paletteDef;
    
    private final BasicStroke borderStroke;
    private final Color ifovSelectedColor;
    private final Color ifovNormalColor;
    private final Color ifovAnomalousColor;

    private int height;
    private final int width;

    private BandInfo bandInfo;
    private ProductData buffer;

    public SounderLayer(SounderOverlay overlay, int productWidth) throws IOException {
        this.overlay = overlay;
        width = productWidth;
        
        borderStroke = new BasicStroke(0.0f);
        ifovSelectedColor = Color.GREEN;
        ifovNormalColor = Color.RED;
        ifovAnomalousColor = Color.WHITE;

        height = overlay.getEpsFile().getMdrCount();
    }

    public BandInfo getBandInfo() {
        return bandInfo;
    }

    public void setBandInfo(BandInfo bandInfo) throws IOException {
        this.bandInfo = bandInfo;
        buffer = ProductData.createInstance(bandInfo.getType(), width * height);
        overlay.getEpsFile().readBandData(bandInfo.getReader(), 0, 0, width, height, buffer , ProgressMonitor.NULL);
        computeColorPalette();
    }

    public SounderOverlay getOverlay() {
        return overlay;
    }
    
    @Override
    public void renderLayer(Rendering rendering) {
        if (overlay.getIfovs().length == 0) {
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

            Rectangle viewRect = getImageRegion(vp);
            
//            final double scale = Math.abs(vp.getModelToViewTransform().getDeterminant());
//            final boolean ifovBigEnough = scale * 47 > 5; // TODO

            final SounderIfov[] ifovs = overlay.getIfovs();
//            if (ifovBigEnough) {
                for (SounderIfov ifov : ifovs) {
                    final Shape ifovShape = ifov.shape;
                    if (ifovShape.intersects(viewRect)) {
                        g2d.setPaint(getColor(ifov.ifovIndex, ifov.mdrIndex));
                        g2d.fill(ifovShape);
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
    
    private static Rectangle getImageRegion(Viewport vp) {
        return vp.getViewToModelTransform().createTransformedShape(vp.getViewBounds()).getBounds();
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

    
//    protected void renderIfov(IasiFootprintLayerModel layerModel, Graphics2D g2d, Ifov ifov, double bt) {
//        final Shape ifovShape = ifov.getShape();
////        boolean selected = layerModel.isSelectedIfov(ifov);
////        g2d.setColor(selected ? ifovSelectedColor : ifov.isAnomalous() ? ifovAnomalousColor : ifovNormalColor);
//        g2d.setPaint(getColor(bt));
//        g2d.fill(ifovShape);
//    }
}
