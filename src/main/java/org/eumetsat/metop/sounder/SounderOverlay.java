/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.eumetsat.metop.sounder;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Debug;
import org.eumetsat.metop.eps.EpsFile;

import javax.swing.SwingWorker;
import java.io.IOException;
import java.util.*;


public abstract class SounderOverlay implements AvhrrOverlay {

    private static final SounderIfov[] NO_DATA = new SounderIfov[0];

    private final EpsFile epsfile;
    private final Product avhrrProduct;
    private final Map<SounderOverlayListener, Object> listenerMap;

    private SounderIfov[] ifovs;
    private boolean loadingIfovs;
    private SounderIfov selectedIfov;

    protected SounderOverlay(EpsFile epsfile, Product avhrrProduct) {
        this.epsfile = epsfile;
        this.avhrrProduct = avhrrProduct;
        // avoid memory leaks (Bloch 2008, Effective Java, Item 6)
        listenerMap = Collections.synchronizedMap(new WeakHashMap<SounderOverlayListener, Object>());
    }

    @Override
    public Product getAvhrrProduct() {
        return avhrrProduct;
    }

    @Override
    public EpsFile getEpsFile() {
        return epsfile;
    }

    public SounderIfov getSelectedIfov() {
        return selectedIfov;
    }

    public void setSelectedIfov(SounderIfov selectedIfov) {
        if (selectedIfov != this.selectedIfov) {
            this.selectedIfov = selectedIfov;
            fireSelectionChanged();
        }
    }

    public void addListener(SounderOverlayListener listener) {
        listenerMap.put(listener, null);
    }

    public void removeListener(SounderOverlayListener listener) {
        listenerMap.remove(listener);
    }

    public SounderIfov[] getIfovs() {
        synchronized (this) {
            if (ifovs != null) {
                return ifovs;
            }
            if (loadingIfovs) {
                return NO_DATA;
            }
            loadingIfovs = true;
        }
        SwingWorker<SounderIfov[], Object> worker = new SwingWorker<SounderIfov[], Object>() {

            @Override
            protected SounderIfov[] doInBackground() throws Exception {
                return readIfovs();
            }

            @Override
            protected void done() {
                try {
                    synchronized (SounderOverlay.this) {
                        loadingIfovs = false;
                        ifovs = get();
                    }
                    fireDataChanged();
                } catch (Exception e) {
                    Debug.trace(e);
                }
            }
        };
        worker.execute();
        return NO_DATA;
    }

    protected abstract SounderIfov[] readIfovs() throws IOException;

    protected void fireSelectionChanged() {
        final Set<SounderOverlayListener> listenerSet;
        synchronized (listenerMap) {
            listenerSet = new HashSet<SounderOverlayListener>(listenerMap.keySet());
        }
        for (final SounderOverlayListener listener : listenerSet) {
            listener.selectionChanged(this);
        }
    }

    protected void fireDataChanged() {
        final Set<SounderOverlayListener> listenerSet;
        synchronized (listenerMap) {
            listenerSet = new HashSet<SounderOverlayListener>(listenerMap.keySet());
        }
        for (final SounderOverlayListener listener : listenerSet) {
            listener.dataChanged(this);
        }
    }

}
