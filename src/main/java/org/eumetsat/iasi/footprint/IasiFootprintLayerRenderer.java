package org.eumetsat.iasi.footprint;


import java.awt.Graphics2D;

import com.bc.ceres.grender.Viewport;

public interface IasiFootprintLayerRenderer {
    void render(IasiFootprintLayer layer, Graphics2D g2d, Viewport viewport);
}
