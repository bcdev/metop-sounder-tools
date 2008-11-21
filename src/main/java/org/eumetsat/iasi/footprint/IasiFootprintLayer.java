/*
 * $Id: SampleRecordsLayer.java,v 1.5 2005/08/16 03:58:04 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.eumetsat.iasi.footprint;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;
import org.eumetsat.iasi.dataio.Efov;
import org.eumetsat.iasi.dataio.Ifov;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public class IasiFootprintLayer extends Layer {

    public static final int EFOV_SIZE = 50;
    public static final int IFOV_SIZE = 12;
    public static final float IFOV_DIST = 18;

    private IasiFootprintLayerModel model;
    private IasiFootprintLayerRenderer renderer;
    private final LayerModelHandler modelListener;

    public IasiFootprintLayer(IasiFootprintLayerModel model) {
        this(model, new DefaultIasiFootprintLayerRenderer());
    }

    public IasiFootprintLayer(IasiFootprintLayerModel model, IasiFootprintLayerRenderer renderer) {
        this.model = model;
        this.renderer = renderer;
        // TODO - re-enable selection (mz, 07.11.2009)
//        setPropertyValue("selectTool", new IfovSelectTool());
        modelListener = new LayerModelHandler();
        model.addListener(modelListener);
    }

    
    @Override
    public String getName() {
        return "IASI L1c ("+getModel().getIasiFile().getFile().getName()+")";
    }
    
    public IasiFootprintLayerModel getModel() {
        return model;
    }

    public void setModel(IasiFootprintLayerModel model) {
        Assert.notNull(model, "model");
        if (this.model != model) {
            this.model.removeListener(modelListener);
            this.model = model;
            this.model.addListener(modelListener);
            fireLayerDataChanged(null);
        }
    }

    public IasiFootprintLayerRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(IasiFootprintLayerRenderer renderer) {
        this.renderer = renderer;
    }

    public Efov[] getEfovs() {
        return getModel().getIasiFile().getEfovs();
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
        try {
            final AffineTransform transform = new AffineTransform();
            transform.concatenate(transformSave);
            transform.concatenate(vp.getModelToViewTransform());
            g2d.setTransform(transform);
            renderer.render(this, g2d, vp);
        } finally {
            g2d.setTransform(transformSave);
        }
    }
    
    @Override
    public void regenerate() {
        if (renderer instanceof DefaultIasiFootprintLayerRenderer) {
            DefaultIasiFootprintLayerRenderer defaultRenderer = (DefaultIasiFootprintLayerRenderer) renderer;
            defaultRenderer.regenerate();
        }
    }

    private class IfovSelectTool extends AbstractTool {

        @Override
        public void mousePressed(ToolInputEvent event) {
            final Ifov clickedIfov = getIfovForLocation(event.getPixelX(), event.getPixelY());
            if (clickedIfov != null) {
                model.setSelectedIfovs(model.isSelectedIfov(clickedIfov) ? new Ifov[0] : new Ifov[]{clickedIfov});
            } else {
                model.setSelectedIfovs(new Ifov[0]);
            }
        }
    }

    private class LayerModelHandler implements IasiFootprintLayerModelListener {
        public void selectionChanged(IasiFootprintLayerModel model) {
            fireLayerDataChanged(null);
        }

        public void dataChanged(IasiFootprintLayerModel model) {
            fireLayerDataChanged(null);
        }
    }
}