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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.AbstractLayerUI;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.math.MathUtils;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.visat.BlackBody;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;


public class SounderLayer extends Layer implements SounderInfo {

    private final AbstractSounderOverlay overlay;
    private final BandInfo[] bandInfos;
    private final Map<Integer,LayerInfo> layerInfoMap = new HashMap<Integer,LayerInfo>();

    private final BasicStroke borderStroke = new BasicStroke(0.0f);
    private final Color ifovSelectedColor = Color.GREEN;

    private final int mdrCount;
    private final int ifovInMdrCount;

    private final SounderOverlayListener listener;

    private volatile int selectedChannel;
    private volatile ProductData layerData;

    protected SounderLayer(AbstractSounderOverlay overlay, BandInfo[] bandInfos, int ifovInMdrCount) throws IOException {
        this.overlay = overlay;
        this.bandInfos = bandInfos;

        this.ifovInMdrCount = ifovInMdrCount;
        this.mdrCount = overlay.getEpsFile().getMdrCount();

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

        selectedChannel = -1;
        setSelectedChannel(0);
    }

    @Override
    protected void disposeLayer() {
        synchronized (this) {
            overlay.removeListener(listener);
            layerData = null;
        }
        super.disposeLayer();
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        if (overlay.getAllIfovs().length == 0) {
            return;
        }
        final ProductData layerData;
        final LayerInfo layerInfo;

        synchronized (this) {
            layerData = getLayerData();
            layerInfo = getLayerInfo();
        }

        final Scaling scaling = layerInfo.getScaling();
        final ImageInfo imageInfo = layerInfo.getImageInfo();
        final Color[] colorPalette = imageInfo.getColorPaletteDef().createColorPalette(scaling);

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

            final Ifov[] ifovs = overlay.getAllIfovs();
            final Ifov selectedIfov = overlay.getSelectedIfov();

            for (final Ifov ifov : ifovs) {
                final Shape ifovShape = ifov.getShape();
                final boolean visible = clip == null || ifovShape.intersects(clip);

                if (visible) {
                    final Color fillColor = getIfovColor(layerData, layerInfo, ifov, colorPalette);
                    g2d.setPaint(fillColor);
                    g2d.fill(ifovShape);

                    if (selectedIfov != null && selectedIfov == ifov) {
                        g2d.setColor(ifovSelectedColor);
                        g2d.draw(ifovShape);
                    }
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

    private ProductData getLayerData() {
        return layerData;
    }

    private LayerInfo getLayerInfo() {
        return layerInfoMap.get(selectedChannel);
    }

    @Override
    public void regenerate() {
        fireLayerDataChanged(getModelBounds());
    }

    @Override
    public SounderOverlay getOverlay() {
        return overlay;
    }

    @Override
    public ImageInfo getImageInfo() {
        return getLayerInfo().getImageInfo();
    }

    @Override
    public Stx getStx() {
        return getLayerInfo().getStx();
    }

    @Override
    public Scaling getScaling() {
        return getLayerInfo().getScaling();
    }

    @Override
    public int getSelectedChannel() {
        return selectedChannel;
    }

    @Override
    public synchronized void setSelectedChannel(final int channel) throws IOException {
        if (selectedChannel != channel) {
            final BandInfo bandInfo = bandInfos[channel];
            final int dataType = bandInfo.getType();
            final ProductData radianceData = ProductData.createInstance(dataType, ifovInMdrCount * mdrCount);

            final MdrReader reader = bandInfo.getReader();
            overlay.getEpsFile().readData(reader, 0, 0, ifovInMdrCount, mdrCount, radianceData);

            layerData = ProductData.createInstance(ProductData.TYPE_FLOAT64, ifovInMdrCount * mdrCount);
            for (int i = 0; i < radianceData.getNumElems(); ++i) {
                final double f = bandInfo.getFrequency();
                final double r = radianceData.getElemDoubleAt(i) * bandInfo.getScaleFactor() * 0.1;
                final double t = BlackBody.temperatureAtFrequency(f, r);
                layerData.setElemDoubleAt(i, t);
            }

            if (layerInfoMap.get(channel) == null) {
                final Band band = new Band("name", ProductData.TYPE_FLOAT64, ifovInMdrCount, mdrCount);
                band.setRasterData(layerData);
                band.setSynthetic(true);

                layerInfoMap.put(channel, LayerInfo.createInstance(band));
            }
            selectedChannel = channel;
        }
    }

    private Ifov getIfovForLocation(int pixelX, int pixelY) {
        final Ifov[] ifovs = overlay.getAllIfovs();
        for (final Ifov ifov : ifovs) {
            if (ifov.getShape().contains(pixelX + 0.5f, pixelY + 0.5f)) {
                return ifov;
            }
        }
        return null;
    }

    private Color getIfovColor(ProductData layerData, LayerInfo layerInfo, Ifov ifov, Color[] colors) {
        final int x = ifov.getIfovInMdrIndex();
        final int y = ifov.getMdrIndex();

        final Scaling scaling = layerInfo.getScaling();
        final double sample = scaling.scale(layerData.getElemDoubleAt(x + y * ifovInMdrCount));
        final int colorCount = colors.length;

        final ImageInfo imageInfo = layerInfo.getImageInfo();
        final ColorPaletteDef paletteDef = imageInfo.getColorPaletteDef();
        final double min = paletteDef.getMinDisplaySample();
        final double max = paletteDef.getMaxDisplaySample();
        final int index = MathUtils.floorAndCrop((sample - min) * (colorCount - 1.0) / (max - min), 0, colorCount - 1);

        return colors[index];
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
            Ifov ifov = selectedSounderLayer.getIfovForLocation(Math.round(point.x), Math.round(point.y));
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
