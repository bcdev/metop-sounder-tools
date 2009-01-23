package org.eumetsat.metop.sounder;

import java.awt.Shape;

/**
 * Interface representing the instantaneous field of view (IFOV) of a
 * sounder.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface Ifov {
    
    int getMdrIndex();

    int getIfovInMdrIndex();

    Shape getShape();
}
