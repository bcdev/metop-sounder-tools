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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


// todo - clean-up
public class SounderLayer extends Layer implements SounderInfo {

    private final AbstractSounderOverlay overlay;
    private final BandInfo[] bandInfos;

    private final int mdrCount;
    private final int ifovInMdrCount;

    private final Map<Integer, LayerData> layerDataMap;
    private final SounderOverlayListener listener;

    private final BasicStroke borderStroke;
    private final Color ifovSelectedColor;

    private volatile int selectedChannel;

    protected SounderLayer(AbstractSounderOverlay overlay, BandInfo[] bandInfos, int ifovInMdrCount) throws IOException {
        this.overlay = overlay;
        this.bandInfos = bandInfos;

        this.ifovInMdrCount = ifovInMdrCount;
        this.mdrCount = overlay.getEpsFile().getMdrCount();

        layerDataMap = new HashMap<Integer, LayerData>();
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
    protected void disposeLayer() {
        overlay.removeListener(listener);
        layerDataMap.clear();
        super.disposeLayer();
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        if (overlay.getAllIfovs().length == 0) {
            return;
        }
        final LayerData layerData = layerDataMap.get(selectedChannel);
        if (layerData == null) {
            final String msg = "renderLayer(): no render info for channel " + selectedChannel;
            System.out.println(msg);
            throw new IllegalStateException(msg);
        }

        final Color[] colorPalette = layerData.imageInfo.getColorPaletteDef().createColorPalette(layerData.band);

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

            final Ifov[] ifovs = overlay.getAllIfovs();
            final Ifov selectedIfov = overlay.getSelectedIfov();
//            if (ifovBigEnough) {
            for (Ifov ifov : ifovs) {
                final Shape ifovShape = ifov.getShape();
                boolean isVisible = clip == null || ifovShape.intersects(clip);
                if (isVisible) {
                    Color fillColor = getColor(layerData, ifov, colorPalette);
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
    public void regenerate() {
        fireLayerDataChanged(getModelBounds());
    }

    @Override
    public SounderOverlay getOverlay() {
        return overlay;
    }

    @Override
    public ImageInfo getImageInfo() {
        return layerDataMap.get(selectedChannel).imageInfo;
    }

    @Override
    public Stx getStx() {
        return layerDataMap.get(selectedChannel).stx;
    }

    @Override
    public Scaling getScaling() {
        return layerDataMap.get(selectedChannel).band;
    }

    @Override
    public int getSelectedChannel() {
        return selectedChannel;
    }

    @Override
    public synchronized void setSelectedChannel(final int channel) throws IOException {
        if (selectedChannel != channel || layerDataMap.isEmpty()) {
            final LayerData layerData = layerDataMap.get(channel);
            if (layerData == null) {
                final BandInfo bandInfo = bandInfos[channel];
                final ProductData data = ProductData.createInstance(bandInfo.getType(),
                                                                    ifovInMdrCount * mdrCount);
                final MdrReader reader = bandInfo.getReader();
                overlay.getEpsFile().readData(reader, 0, 0, ifovInMdrCount, mdrCount, data, ProgressMonitor.NULL);

                final Band band = new Band(bandInfo.getName(), bandInfo.getType(), ifovInMdrCount, mdrCount);
                band.setRasterData(data);
                band.setSynthetic(true);
                // todo - scaling
                final Stx stx = Stx.create(band, 0, ProgressMonitor.NULL);

                layerDataMap.put(channel, new LayerData(band, stx));
            }
            selectedChannel = channel;
            fireLayerDataChanged(null);
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

    private Color getColor(LayerData layerData, Ifov ifov, Color[] colors) {
        final ColorPaletteDef def = layerData.imageInfo.getColorPaletteDef();
        final double sample = layerData.band.getData().getElemDoubleAt(
                ifov.getIfovInMdrIndex() + ifov.getMdrIndex() * ifovInMdrCount);
        final int numColors = colors.length;

        final double min = def.getMinDisplaySample();
        final double max = def.getMaxDisplaySample();
        final int index = MathUtils.floorAndCrop((sample - min) * (numColors - 1.0) / (max - min), 0, numColors - 1);

        return colors[index];
    }

    private static class LayerData {

        final Band band;
        final Stx stx;
        final ImageInfo imageInfo;

        private LayerData(Band band, Stx stx) {
            this.stx = stx;
            this.band = band;
            this.imageInfo = createImageInfo(stx);
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

    private static ImageInfo createImageInfo(Stx stx) {
        return new ImageInfo(new ColorPaletteDef(stx.getMin(), stx.getMean(), stx.getMax()));
    }

}
