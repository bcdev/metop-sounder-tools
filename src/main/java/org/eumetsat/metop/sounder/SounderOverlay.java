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
import org.eumetsat.metop.eps.EpsFile;


public abstract class SounderOverlay implements AvhrrOverlay {

    private final EpsFile epsfile;
    private final Product avhrrProduct;

    public SounderOverlay(EpsFile epsfile, Product avhrrProduct) {
        this.epsfile = epsfile;
        this.avhrrProduct = avhrrProduct;
    }
    
    public Product getAvhrrProduct() {
        return avhrrProduct;
    }

    public EpsFile getEpsFile() {
        return epsfile;
    }

    public abstract SounderIfov[] getIfovs();
}