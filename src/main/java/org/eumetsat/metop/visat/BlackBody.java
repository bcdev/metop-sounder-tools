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

import static java.lang.Math.log;

/**
 * Utility class for calculating physical quantities for a black body.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class BlackBody {

    // usefull for deriving conversion constants
    @SuppressWarnings({"UnusedDeclaration"})
    private static final double C = 299792458.0; // speed of light (ms-1)
    @SuppressWarnings({"UnusedDeclaration"})
    private static final double H = 6.62606896E-34; // Planck constant (Js)
    @SuppressWarnings({"UnusedDeclaration"})
    private static final double K = 1.38065040E-23; // Boltzmann constant (JK-1)

    private BlackBody() {
    }

    /**
     * Calculates the black-body temperature for a spectral black-body
     * radiance at a certain wavenumber.
     *
     * @param k the wavenumber (m-1)
     * @param i the spectral radiance (W/m2/sr/m-1)
     *
     * @return the black-body temperature (K)
     */
    static double temperatureAtWavenumber(double k, double i) {
        final double c1 = 1.1910427584934558E-16;
        final double c2 = 1.4387751601679205E-02;
        final double a = c2 * k;
        final double b = c1 * k * k * k;

        return a / (log(1.0 + (b / i)));
    }

    /**
     * Calculates the black-body temperature for a spectral black-body
     * radiance at a certain frequency.
     *
     * @param f the frequency (GHz)
     * @param i the radiance (W/m2/sr/m-1)
     *
     * @return the black-body temperature (K)
     */
    public static double temperatureAtFrequency(double f, double i) {
        final double c1 = 4.9183999150233067E-14;
        final double c2 = 4.7992373449498874E-02;
        final double a = c2 * f;
        final double b = c1 * f * f * f * f * f;

        return a / (log(1.0 + (b / i)));
    }

}
