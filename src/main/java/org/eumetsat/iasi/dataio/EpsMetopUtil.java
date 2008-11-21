/* 
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.iasi.dataio;

import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision: 79 $ $Date: 2008-01-10 11:23:38 +0100 (Do, 10 Jan 2008) $
 */
class EpsMetopUtil {

    public static ProductData.UTC readShortCdsTime(ImageInputStream iis) throws IOException {
        final int day = iis.readUnsignedShort();
        final long millis = iis.readUnsignedInt();

        final long seconds = millis / 1000;
        final long micros = (millis - seconds * 1000) * 1000;
        
        return new ProductData.UTC(day, (int) seconds, (int) micros);
    }

    public static double readVInt4(ImageInputStream iis) throws IOException {
        final byte scaleFactor = iis.readByte();
        final int value = iis.readInt();

        return value / Math.pow(10.0, scaleFactor);
    }
}
