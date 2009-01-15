/* 
 * Copyright (C) 2002-2007 by Brockmann Consult
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

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;
import org.eumetsat.iasi.footprint.DefaultIasiFootprintLayerRenderer;
import org.eumetsat.iasi.footprint.IasiFootprintLayer;
import org.eumetsat.iasi.footprint.IasiFootprintLayerRenderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

import javax.swing.JComponent;
import javax.swing.JPanel;


public class IfovColorView extends AbstractToolView {

    public static final int INVALID_INDEX = -1;
    public static final int PALETTE_HEIGHT = 16;
    public static final int SLIDER_WIDTH = 12;
    public static final int SLIDER_HEIGHT = 10;
    public static final int SLIDER_VALUES_AREA_HEIGHT = 80;
    public static final int HOR_BORDER_SIZE = 10;
    public static final int VER_BORDER_SIZE = 4;
    public static final int PREF_HISTO_WIDTH = 256; //196;
    public static final int PREF_HISTO_HEIGHT = 196; //128;
    public static final int FONT_SIZE = 9;
    
    public static final BasicStroke STROKE_1 = new BasicStroke(1.0f);
    public static final BasicStroke STROKE_2 = new BasicStroke(2.0f);
    public static final BasicStroke DASHED_STROKE = new BasicStroke(0.75F, BasicStroke.CAP_SQUARE,
                                                                    BasicStroke.JOIN_MITER, 1.0F, new float[]{5.0F},
                                                                    0.0F);
    
    private final Rectangle paletteRect;
    private JPanel myPanel;
    private Rectangle sliderBaseLineRect;
    private Shape sliderShape;

    public IfovColorView() {
        paletteRect = new Rectangle();
        sliderBaseLineRect = new Rectangle();
        sliderShape = createSliderShape();
    }
    
    public static Shape createSliderShape() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0.0F, -0.5F * SLIDER_HEIGHT);
        path.lineTo(+0.5F * SLIDER_WIDTH, 0.5F * SLIDER_HEIGHT);
        path.lineTo(-0.5F * SLIDER_WIDTH, 0.5F * SLIDER_HEIGHT);
        path.closePath();
        return path;
    }
    
    @Override
    protected JComponent createControl() {
        myPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintMyStuff(g);
            }
        };
        return myPanel;
    }
    
    private void paintMyStuff(Graphics g) {
        final ProductSceneView psv = VisatApp.getApp().getSelectedProductSceneView();
        if (psv != null && IasiFootprintVPI.isValidAvhrrProductSceneView(psv)) {
            IasiFootprintLayer layer = IasiFootprintVPI.getActiveFootprintLayer(IasiFootprintLayer.class);
            if (layer != null) {
                IasiFootprintLayerRenderer iasiFootprintLayerRenderer = layer.getRenderer();
                DefaultIasiFootprintLayerRenderer rendere = (DefaultIasiFootprintLayerRenderer) iasiFootprintLayerRenderer;
                Color[] colorPalette = rendere.getColorPalette();
                ColorPaletteDef colorPaletteDef = rendere.getColorPaletteDef();
                double roundFactor = MathUtils.computeRoundFactor(colorPaletteDef.getMinDisplaySample(), colorPaletteDef.getMaxDisplaySample(), 2);
                Graphics2D g2d = (Graphics2D) g;
                if (colorPalette != null) {
                    computeSizeAttributes();
                    long paletteX1 = paletteRect.x;
                    long paletteX2 = paletteRect.x + paletteRect.width;

                    for (int x = paletteRect.x; x < paletteRect.x + paletteRect.width; x++) {
                        long divisor = paletteX2 - paletteX1;
                        int palIndex;
                        if (divisor == 0) {
                            palIndex = x < paletteX1 ? 0 : colorPalette.length - 1;
                        } else {
                            palIndex = (int) ((colorPalette.length * (x - paletteX1)) / divisor);
                        }
                        if (palIndex < 0) {
                            palIndex = 0;
                        }
                        if (palIndex > colorPalette.length - 1) {
                            palIndex = colorPalette.length - 1;
                        }
                        g2d.setColor(colorPalette[palIndex]);
                        g2d.drawLine(x, paletteRect.y, x, paletteRect.y + paletteRect.height);
                    }
                    g2d.translate(sliderBaseLineRect.x, sliderBaseLineRect.y);
                    g2d.setStroke(STROKE_1);
                    
//                    for (int i = 0; i < getSliderCount(); i++) {
//                        double sliderPos = getRelativeSliderPos(getSliderSample(i));

//                        g2d.translate(0.0, 0.0);

                        Color sliderColor = colorPalette[0];
                        g2d.setPaint(sliderColor);
                        g2d.fill(sliderShape);

                        int gray = (sliderColor.getRed() + sliderColor.getGreen() + sliderColor.getBlue()) / 3;
                        g2d.setColor(gray < 128 ? Color.white : Color.black);
                        g2d.draw(sliderShape);

                        String text = String.valueOf(MathUtils.round(colorPaletteDef.getMinDisplaySample(), roundFactor));
                        g2d.setColor(Color.black);
                        // save the old transformation
                        final AffineTransform oldTransform = g2d.getTransform();
                        g2d.transform(AffineTransform.getRotateInstance(Math.PI / 2));
                        g2d.drawString(text, 3 + 0.5f * SLIDER_HEIGHT, 0.35f * FONT_SIZE);
                        // restore the old transformation
                        g2d.setTransform(oldTransform);
                        
                        g2d.translate(sliderBaseLineRect.width, 0.0);
                        
                        sliderColor = colorPalette[colorPalette.length-1];
                        g2d.setPaint(sliderColor);
                        g2d.fill(sliderShape);

                        g2d.setColor(gray < 128 ? Color.white : Color.black);
                        g2d.draw(sliderShape);
                        
                        text = String.valueOf(MathUtils.round(colorPaletteDef.getMaxDisplaySample(), roundFactor));
                        g2d.setColor(Color.black);
                        g2d.transform(AffineTransform.getRotateInstance(Math.PI / 2));
                        g2d.drawString(text, 3 + 0.5f * SLIDER_HEIGHT, 0.35f * FONT_SIZE);
                        // restore the old transformation
                        g2d.setTransform(oldTransform);

//                        g2d.translate(-sliderPos, 0.0);
                        g2d.translate(-sliderBaseLineRect.width, 0.0);
//                    }

                    g2d.translate(-sliderBaseLineRect.x, -sliderBaseLineRect.y);
                    
                }
            }
        }
    }
    
    private void computeSizeAttributes() {
        int totWidth = myPanel.getWidth();
        int totHeight = myPanel.getHeight();

        int imageWidth = totWidth - 2 * HOR_BORDER_SIZE;

        int sliderTextBaseLineY = totHeight - VER_BORDER_SIZE - SLIDER_VALUES_AREA_HEIGHT;

        sliderBaseLineRect .x = HOR_BORDER_SIZE;
        sliderBaseLineRect.y = sliderTextBaseLineY - SLIDER_HEIGHT / 2;
        sliderBaseLineRect.width = imageWidth;
        sliderBaseLineRect.height = 1;

        paletteRect.x = HOR_BORDER_SIZE;
        paletteRect.y = sliderBaseLineRect.y - PALETTE_HEIGHT;
        paletteRect.width = imageWidth;
        paletteRect.height = PALETTE_HEIGHT;
    }
}
