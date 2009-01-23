package org.eumetsat.metop.iasi;

import org.eumetsat.metop.sounder.Ifov;

import java.awt.Shape;

public final class IasiIfov implements Ifov {

    private final int ifovIndex;
    private final float pixelX;
    private final float pixelY;
    private final Shape shape;
    private final boolean anomalous;
    private volatile Efov efov;

    public IasiIfov(int ifovIndex, float pixelX, float pixelY, Shape shape) {
        this(ifovIndex, pixelX, pixelY, shape, false);
    }

    public IasiIfov(int ifovIndex, float pixelX, float pixelY, Shape shape, boolean anomalous) {
        this.ifovIndex = ifovIndex;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.shape = shape;
        this.anomalous = anomalous;
    }

    @Override
    public int getMdrIndex() {
        return ifovIndex % 120;
    }

    @Override
    public int getIfovInMdrIndex() {
        return ifovIndex - getMdrIndex();
    }

    @Override
    public final Shape getShape() {
        return shape;
    }

    public final int getIfovIndex() {
        return ifovIndex;
    }

    public final Efov getEfov() {
        return efov;
    }

    final void setEfov(Efov efov) {
        this.efov = efov;
    }

    public final float getPixelX() {
        return pixelX;
    }

    public final float getPixelY() {
        return pixelY;
    }

    public final boolean isAnomalous() {
        return anomalous;
    }
}
