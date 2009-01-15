/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.eumetsat.metop.visat;

import org.eumetsat.metop.mhs.MhsSounderLayer;
import org.eumetsat.metop.sounder.SounderLayer;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.Range;

/**
 * Tool view for showing information on the selected MHS field-of-view.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class MhsInfoView extends SounderInfoView {

    @Override
    protected SounderLayer getActiveSounderLayer() {
        return IasiFootprintVPI.getActiveFootprintLayer(MhsSounderLayer.class);
    }

    @Override
    protected double[] getSpectrumAbscissaValues() {
        // todo - implement
        return new double[0];
    }

    @Override
    protected NumberAxis createSpectrumPlotXAxis() {
        // todo - implement
        final NumberAxis axis = new NumberAxis("Wavenumber (cm-1)");
        axis.setRange(645.0, 2760.0);

        return axis;
    }

    @Override
    protected NumberAxis createSpectrumPlotYAxis() {
        // todo - implement
        final NumberAxis axis = new NumberAxis("Scene Radiance (mW/m2/sr/cm-1)");
        axis.setRange(new Range(180.0, 305.0));

        return axis;
    }
}
