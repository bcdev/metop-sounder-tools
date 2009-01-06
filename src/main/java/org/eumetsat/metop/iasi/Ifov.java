package org.eumetsat.metop.iasi;

import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

public class Ifov {

    private volatile Efov efov;
    private final int index;
    private final float pixelX;
    private final float pixelY;
    private final Shape shape;
    private final boolean anomalous;

    public Ifov(int index, float pixelX, float pixelY, Shape shape) {
        this(index, pixelX, pixelY, shape, false);
    }

    public Ifov(int index, float pixelX, float pixelY, Shape shape, boolean anomalous) {
        this.index = index;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.shape = shape;
        this.anomalous = anomalous;
    }

    public int getIndex() {
        return index;
    }

    public Efov getEfov() {
        return efov;
    }

    public void setEfov(Efov efov) {
        this.efov = efov;
    }

    public float getPixelX() {
        return pixelX;
    }

    public float getPixelY() {
        return pixelY;
    }

    public Shape getShape() {
        return shape;
    }

    public boolean isAnomalous() {
        return anomalous;
    }
}
