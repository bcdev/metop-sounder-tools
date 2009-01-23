package org.eumetsat.metop.iasi;

import java.awt.Shape;

public final class IasiIfov {

    private volatile Efov efov;
    private final int index;
    private final float pixelX;
    private final float pixelY;
    private final Shape shape;
    private final boolean anomalous;

    public IasiIfov(int index, float pixelX, float pixelY, Shape shape) {
        this(index, pixelX, pixelY, shape, false);
    }

    public IasiIfov(int index, float pixelX, float pixelY, Shape shape, boolean anomalous) {
        this.index = index;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.shape = shape;
        this.anomalous = anomalous;
    }

    public final int getIndex() {
        return index;
    }

    public final Efov getEfov() {
        return efov;
    }

    public final void setEfov(Efov efov) {
        this.efov = efov;
    }

    public final float getPixelX() {
        return pixelX;
    }

    public final float getPixelY() {
        return pixelY;
    }

    public final Shape getShape() {
        return shape;
    }

    public final boolean isAnomalous() {
        return anomalous;
    }
}
