package org.eumetsat.metop.sounder;

import java.awt.Shape;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public interface Ifov {
    int getMdrIndex();

    int getIfovInMdrIndex();

    Shape getShape();
}
