package org.eumetsat.iasi.footprint;

import org.eumetsat.metop.iasi.IasiOverlay;
import org.eumetsat.metop.iasi.Ifov;


public interface IasiFootprintLayerModel {
    IasiOverlay getIasiOverlay();

    boolean isSelectedIfov(Ifov ifov);

    void setSelectedIfov(Ifov ifov, boolean selected);

    boolean isSelectionEmpty();

    void dispose();

    void addListener(IasiFootprintLayerModelListener modelListener);

    void removeListener(IasiFootprintLayerModelListener modelListener);

    void setSelectedIfovs(Ifov[] selectedIfovs);

    Ifov[] getSelectedIfovs();
}
