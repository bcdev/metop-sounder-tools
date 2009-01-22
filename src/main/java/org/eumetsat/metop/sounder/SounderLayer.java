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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class SounderLayer extends Layer {

    private final SounderOverlay overlay;
    private final BandInfo[] bandInfos;

    private final int mdrCount;
    private final int ifovInMdrCount;

    private final Map<Integer, RenderInfo> renderInfoMap;
    private final SounderOverlayListener listener;

    private final BasicStroke borderStroke;
    private final Color ifovSelectedColor;

    private volatile int selectedChannel;

    protected SounderLayer(SounderOverlay overlay, BandInfo[] bandInfos, int ifovInMdrCount) throws IOException {
        this.overlay = overlay;
        this.bandInfos = bandInfos;

        this.ifovInMdrCount = ifovInMdrCount;
        this.mdrCount = overlay.getEpsFile().getMdrCount();

        renderInfoMap = Collections.synchronizedMap(new HashMap<Integer, RenderInfo>());
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

        borderStroke = new BasicStroke(0.0f);
        ifovSelectedColor = Color.GREEN;

        setSelectedChannel(0);
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        if (overlay.getIfovs().length == 0) {
            return;
        }
        final RenderInfo renderInfo = renderInfoMap.get(selectedChannel);
        if (renderInfo == null) {
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
                    Color fillColor = getColor(renderInfo, ifov);
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
        renderInfoMap.clear();
        super.disposeLayer();
    }

    public SounderOverlay getOverlay() {
        return overlay;
    }

    public ImageInfo getImageInfo() {
        final RenderInfo data = renderInfoMap.get(selectedChannel);

        if (data == null) {
            return null;
        }

        return data.imageInfo;
    }

    public int getSelectedChannel() {
        return selectedChannel;
    }

    public void setSelectedChannel(final int channel) {
        if (selectedChannel != channel || renderInfoMap.isEmpty()) {
            final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {
                    synchronized (renderInfoMap) {
                        final RenderInfo renderInfo = renderInfoMap.get(channel);
                        if (renderInfo != null) {
                            return null;
                        }

                        final BandInfo bandInfo = bandInfos[channel];
                        final ProductData data = ProductData.createInstance(bandInfo.getType(),
                                                                            ifovInMdrCount * mdrCount);
                        final MdrReader reader = bandInfo.getReader();
                        overlay.getEpsFile().readData(reader, 0, 0, ifovInMdrCount, mdrCount, data,
                                                      ProgressMonitor.NULL);

                        final ColorPaletteDef colorPaletteDef = createColorPaletteDef(data);
                        final ImageInfo imageInfo = new ImageInfo(colorPaletteDef);

                        renderInfoMap.put(channel, new RenderInfo(data, imageInfo));
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

    private SounderIfov getIfovForLocation(int pixelX, int pixelY) {
        final SounderIfov[] ifovs = overlay.getIfovs();
        for (final SounderIfov ifov : ifovs) {
            if (ifov.shape.contains(pixelX + 0.5f, pixelY + 0.5f)) {
                return ifov;
            }
        }
        return null;
    }

    private Color getColor(RenderInfo renderInfo, SounderIfov ifov) {
        final Color[] colors = renderInfo.imageInfo.getColors();
        final ColorPaletteDef paletteDef = renderInfo.imageInfo.getColorPaletteDef();
        final double sample = renderInfo.data.getElemDoubleAt(ifov.ifovInMdrIndex + ifov.mdrIndex * ifovInMdrCount);
        final int numColors = colors.length;

        final double min = paletteDef.getMinDisplaySample();
        final double max = paletteDef.getMaxDisplaySample();
        final int index = MathUtils.floorAndCrop((sample - min) * (numColors - 1.0) / (max - min), 0, numColors - 1);

        return colors[index];
    }

    private static class RenderInfo {

        final ProductData data;

        final ImageInfo imageInfo;

        private RenderInfo(ProductData data, ImageInfo imageInfo) {
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
