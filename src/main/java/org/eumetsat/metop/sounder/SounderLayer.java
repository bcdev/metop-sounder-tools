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

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.AbstractLayerUI;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.math.MathUtils;

import javax.swing.SwingWorker;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class SounderLayer extends Layer {

    private final SounderOverlay overlay;
    private final BasicStroke borderStroke;
    private final Color ifovSelectedColor;

    private final int height;
    private final int width;

    private final BandInfo[] bandInfos;
    private final Map<Integer, LayerData> layerDataMap = new HashMap<Integer, LayerData>();
    private final SounderOverlayListener listener;

    private int selectedChannel;

    public SounderLayer(SounderOverlay overlay, BandInfo[] bandInfos, int productWidth) throws IOException {
        this.overlay = overlay;
        this.bandInfos = bandInfos;

        borderStroke = new BasicStroke(0.0f);
        ifovSelectedColor = Color.GREEN;

        width = productWidth;
        height = overlay.getEpsFile().getMdrCount();

        setSelectedChannel(0);
        listener = new SounderOverlayListener() {
            @Override
            public void dataChanged(SounderOverlay overlay) {
                fireLayerDataChanged(null);
            }

            @Override
            public void selectionChanged(SounderOverlay overlay) {
                fireLayerDataChanged(null);
            }
        };
        overlay.addListener(listener);
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        if (overlay.getIfovs().length == 0) {
            return;
        }
        final LayerData layerData = layerDataMap.get(selectedChannel);
        if (layerData == null) {
            throw new IllegalStateException("No layer data");
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
                    Color fillColor = getColor(layerData, ifov);
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

    @Override
    protected void disposeLayer() {
        overlay.removeListener(listener);
        layerDataMap.clear();
        super.disposeLayer();
    }

    public SounderOverlay getOverlay() {
        return overlay;
    }

    private SounderIfov getIfovForLocation(int pixelX, int pixelY) {
        final SounderIfov[] ifovs = overlay.getIfovs();
        for (final SounderIfov ifov : ifovs) {
            if (ifov.shape.contains(pixelX + 0.5f, pixelY + 0.5f)) {
                return ifov;
            }
        }
        return null;
    }

    private Color getColor(LayerData layerData, SounderIfov ifov) {
        final Color[] colors = layerData.imageInfo.getColors();
        final ColorPaletteDef paletteDef = layerData.imageInfo.getColorPaletteDef();
        final double sample = layerData.data.getElemDoubleAt(ifov.ifovInMdrIndex + ifov.mdrIndex * width);
        final int numColors = colors.length;

        final double min = paletteDef.getMinDisplaySample();
        final double max = paletteDef.getMaxDisplaySample();
        final int index = MathUtils.floorAndCrop((sample - min) * (numColors - 1.0) / (max - min), 0, numColors - 1);

        return colors[index];
    }

    public void setSelectedChannel(final int channel) {
        if (selectedChannel != channel || layerDataMap.isEmpty()) {
            // todo - use ProgressMonitorSwingWorker instead?
            final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {
                    synchronized (layerDataMap) {
                        final LayerData layerData = layerDataMap.get(channel);
                        if (layerData != null) {
                            return null;
                        }

                        final BandInfo bandInfo = bandInfos[channel];
                        final ProductData data = ProductData.createInstance(bandInfo.getType(), width * height);
                        final MdrReader reader = bandInfo.getReader();
                        overlay.getEpsFile().readData(reader, 0, 0, width, height, data, ProgressMonitor.NULL);

                        final ColorPaletteDef colorPaletteDef = createColorPaletteDef(data);
                        final ImageInfo imageInfo = new ImageInfo(colorPaletteDef);

                        layerDataMap.put(channel, new LayerData(data, imageInfo));
                        return null;
                    }
                }

                @Override
                protected void done() {
                    try {
                        selectedChannel = channel;
                        fireLayerDataChanged(null);
                    } catch (Exception e) {
                        Debug.trace(e);
                    }
                }
            };

            worker.execute();
        }
    }

    public ImageInfo getImageInfo() {
        final LayerData data = layerDataMap.get(selectedChannel);

        if (data == null) {
            return null;
        }

        return data.imageInfo;
    }

    private static class LayerData {

        final ProductData data;

        final ImageInfo imageInfo;

        private LayerData(ProductData data, ImageInfo imageInfo) {
            this.imageInfo = imageInfo;
            this.data = data;
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

    private static ColorPaletteDef createColorPaletteDef(ProductData data) {
        int valueMin = Integer.MAX_VALUE;
        int valueMax = Integer.MIN_VALUE;

        for (int i = 0; i < data.getNumElems(); i++) {
            final int value = data.getElemIntAt(i);
            valueMin = Math.min(valueMin, value);
            valueMax = Math.max(valueMax, value);
        }

        return new ColorPaletteDef(valueMin, valueMax);
    }

}
