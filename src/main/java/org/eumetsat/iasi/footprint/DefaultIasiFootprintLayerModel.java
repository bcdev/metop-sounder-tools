package org.eumetsat.iasi.footprint;

import org.eumetsat.metop.iasi.Efov;
import org.eumetsat.metop.iasi.IasiOverlay;
import org.eumetsat.metop.iasi.IasiFile;
import org.eumetsat.metop.iasi.Ifov;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides the layer data for the {@link IasiFootprintLayer}.
 */
public class DefaultIasiFootprintLayerModel implements IasiFootprintLayerModel {

    private final ArrayList<Ifov> selectedIfovList;
    private final ArrayList<IasiFootprintLayerModelListener> modelListeners;
    private final IasiOverlay iasiOverlay;

    public DefaultIasiFootprintLayerModel(IasiOverlay iasiOverlay) {
        this.iasiOverlay = iasiOverlay;
        selectedIfovList = new ArrayList<Ifov>(61);
        modelListeners = new ArrayList<IasiFootprintLayerModelListener>(3);
    }

    public synchronized IasiOverlay getIasiOverlay() {
        return iasiOverlay;
    }

    public synchronized Efov[] getEfovs() {
        return iasiOverlay.getEfovs();
    }

    public synchronized boolean isSelectedIfov(Ifov ifov) {
        return selectedIfovList.contains(ifov);
    }

    public synchronized void setSelectedIfovs(Ifov[] ifovs) {
        final List<Ifov> ifovList = Arrays.asList(ifovs);
        final boolean change = ifovs.length != selectedIfovList.size() || !selectedIfovList.containsAll(ifovList);

        if (change) {
            selectedIfovList.clear();
            selectedIfovList.addAll(ifovList);
            fireSelectionChange();
        }
    }

    public Ifov[] getSelectedIfovs() {
        return selectedIfovList.toArray(new Ifov[selectedIfovList.size()]);
    }

    public synchronized void setSelectedIfov(Ifov ifov, boolean selected) {
        if (selected) {
            if (selectedIfovList.add(ifov)) {
                fireSelectionChange();
            }
        } else {
            if (selectedIfovList.remove(ifov)) {
                fireSelectionChange();
            }
        }
    }

    public synchronized void clearSelection() {
        if (selectedIfovList.size() > 0) {
            selectedIfovList.clear();
            fireSelectionChange();
        }
    }

    public synchronized boolean isSelectionEmpty() {
        return selectedIfovList.isEmpty();
    }

    public synchronized void dispose() {
        iasiOverlay.close();
        selectedIfovList.clear();
        modelListeners.clear();
    }

    public synchronized void addListener(IasiFootprintLayerModelListener modelListener) {
        modelListeners.add(modelListener);

    }

    public synchronized void removeListener(IasiFootprintLayerModelListener modelListener) {
        modelListeners.remove(modelListener);
    }

    protected synchronized void fireSelectionChange() {
        for (IasiFootprintLayerModelListener modelListener : modelListeners) {
            modelListener.selectionChanged(this);
        }
    }

    protected synchronized void fireDataChange() {
        for (IasiFootprintLayerModelListener modelListener : modelListeners) {
            modelListener.dataChanged(this);
        }
    }

}
