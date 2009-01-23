package org.eumetsat.metop.sounder;

/**
 * Representation of a sounder overlay for AVHRR products.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface SounderOverlay extends AvhrrOverlay {

    Ifov getSelectedIfov();

    void setSelectedIfov(Ifov ifov);

    void addListener(SounderOverlayListener listener);

    void removeListener(SounderOverlayListener listener);

    
}
