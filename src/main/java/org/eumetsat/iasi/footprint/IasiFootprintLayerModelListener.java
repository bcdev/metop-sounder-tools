package org.eumetsat.iasi.footprint;

public interface IasiFootprintLayerModelListener {
    void selectionChanged(IasiFootprintLayerModel model);
    void dataChanged(IasiFootprintLayerModel model);
}
