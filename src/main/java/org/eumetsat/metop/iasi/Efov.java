package org.eumetsat.metop.iasi;

import java.awt.*;

public class Efov {
    private final int index;
    private final Ifov[] ifovs;
    private final Shape shape;

    public Efov(int index, Ifov[] ifovs, Shape shape) {
        this.index = index;
        this.ifovs = ifovs.clone();
        this.shape = shape;

        for (Ifov ifov : this.ifovs) {
            ifov.setEfov(this);
        }
    }

    public int getIndex() {
        return index;
    }

    public Ifov[] getIfovs() {
        return ifovs;
    }

    public Shape getShape() {
        return shape;
    }

    public final boolean isAnyIfovAnomalous() {
        for (final Ifov ifov : getIfovs()) {
            if (ifov.isAnomalous()) {
                return true;
            }
        }
        return false;
    }
}
