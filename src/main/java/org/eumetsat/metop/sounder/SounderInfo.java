package org.eumetsat.metop.sounder;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.datamodel.Scaling;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public interface SounderInfo {
    SounderOverlay getOverlay();

    ImageInfo getImageInfo();

    Stx getStx();

    Scaling getScaling();

    int getSelectedChannel();

    void setSelectedChannel(int channel);
}
