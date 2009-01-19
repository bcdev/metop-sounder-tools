package org.eumetsat.metop.iasi;


public interface IasiOverlayListener {
    void selectionChanged(IasiOverlay overlay);
    void dataChanged(IasiOverlay overlay);
}
