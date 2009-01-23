package org.eumetsat.metop.iasi;

import java.awt.*;

public class Efov {
    private final int index;
    private final IasiIfov[] ifovs;
    private final Shape shape;

    public Efov(int index, IasiIfov[] ifovs, Shape shape) {
        this.index = index;
        this.ifovs = ifovs.clone();
        this.shape = shape;

        for (IasiIfov ifov : this.ifovs) {
            ifov.setEfov(this);
        }
    }

    public int getIndex() {
        return index;
    }

    public IasiIfov[] getIfovs() {
        return ifovs;
    }

    public Shape getShape() {
        return shape;
    }

    public final boolean isAnyIfovAnomalous() {
        for (final IasiIfov ifov : getIfovs()) {
            if (ifov.isAnomalous()) {
                return true;
            }
        }
        return false;
    }
}
