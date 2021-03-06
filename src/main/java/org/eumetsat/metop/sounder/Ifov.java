package org.eumetsat.metop.sounder;

import java.awt.Shape;

/**
 * Interface representing the instantaneous field of view (IFOV) of a
 * sounder instrument.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface Ifov {
    
    int getMdrIndex();

    int getIfovIndex();
    
    int getIfovInMdrIndex();

    Shape getShape();
}
