/*
 * Copyright (C) 2007 by Eumetsat
 */
package org.eumetsat.iasi.dataio;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @version $Revision: 80 $ $Date: 2008-01-10 11:26:23 +0100 (Do, 10 Jan 2008) $
 */
public class IasiReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "IASI_L1C";
    private static final String DESCRIPTION = "METOP-IASI Level-1C Data Product";

    public DecodeQualification getDecodeQualification(Object input) {
        DecodeQualification decodeQualification = DecodeQualification.UNABLE;

        if (input instanceof File) {
            try {
                if (IasiFile.isIasiFile((File) input)) {
                    decodeQualification = DecodeQualification.INTENDED;
                }
            } catch (IOException e) {
                // ignore
            }
        }

        return decodeQualification;
    }

    public Class[] getInputTypes() {
        return new Class[]{File.class};
    }

    public ProductReader createReaderInstance() {
        return new IasiReader(this);
    }

    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    public String[] getDefaultFileExtensions() {
        return new String[]{".nat"};
    }

    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }
}
