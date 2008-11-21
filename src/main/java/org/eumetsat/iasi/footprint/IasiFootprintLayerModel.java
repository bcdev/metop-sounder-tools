package org.eumetsat.iasi.footprint;

import org.eumetsat.iasi.dataio.IasiFile;
import org.eumetsat.iasi.dataio.Efov;
import org.eumetsat.iasi.dataio.Ifov;

public interface IasiFootprintLayerModel {
    IasiFile getIasiFile();

    boolean isSelectedIfov(Ifov ifov);

    void setSelectedIfov(Ifov ifov, boolean selected);

    boolean isSelectionEmpty();

    void dispose();

    void addListener(IasiFootprintLayerModelListener modelListener);

    void removeListener(IasiFootprintLayerModelListener modelListener);

    void setSelectedIfovs(Ifov[] selectedIfovs);

    Ifov[] getSelectedIfovs();
}
