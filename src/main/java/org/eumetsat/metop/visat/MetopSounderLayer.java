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
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import org.eumetsat.metop.amsu.AmsuAvhrrOverlay;
import org.eumetsat.metop.amsu.AmsuIfov;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;


public class MetopSounderLayer extends Layer {

    private final AmsuAvhrrOverlay overlay;
    
    private final BasicStroke borderStroke;
    private final Color efovColor;
    private final Color ifovSelectedColor;
    private final Color ifovNormalColor;
    private final Color ifovAnomalousColor;

    public MetopSounderLayer(AmsuAvhrrOverlay overlay) {
        this.overlay = overlay;
        borderStroke = new BasicStroke(0.0f);
        efovColor = Color.WHITE;
        ifovSelectedColor = Color.GREEN;
        ifovNormalColor = Color.RED;
        ifovAnomalousColor = Color.WHITE;
    }

    public AvhrrOverlay getOverlay() {
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

            final Rectangle clip = g2d.getClipBounds();

            
            final double scale = Math.abs(vp.getModelToViewTransform().getDeterminant());
            final boolean ifovBigEnough = scale * 47 > 5; // TODO

            final AmsuIfov[] ifovs = overlay.getIfovs();
            
            if (ifovBigEnough) {
                for (AmsuIfov ifov : ifovs) {
                    final Shape ifovShape = ifov.getShape();
                    g2d.setPaint(overlay.getColor(ifov.getValue()));
                    g2d.fill(ifovShape);
                }
            }

            g2d.setColor(oldColor);
            g2d.setPaint(oldPaint);
            g2d.setStroke(oldStroke);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, oldRendering);
        } finally {
            g2d.setTransform(transformSave);
        }
    }
    
//    protected void renderIfov(IasiFootprintLayerModel layerModel, Graphics2D g2d, Ifov ifov, double bt) {
//        final Shape ifovShape = ifov.getShape();
////        boolean selected = layerModel.isSelectedIfov(ifov);
////        g2d.setColor(selected ? ifovSelectedColor : ifov.isAnomalous() ? ifovAnomalousColor : ifovNormalColor);
//        g2d.setPaint(getColor(bt));
//        g2d.fill(ifovShape);
//    }
}
