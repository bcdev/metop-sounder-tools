/* 
 * Copyright (C) 2002-2007 by Brockmann Consult
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
package org.eumetsat.metop.iasi;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.ProductData;
import org.eumetsat.metop.eps.EpsFile;
import org.eumetsat.metop.eps.EpsFormats;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class IasiFileTest extends TestCase {

    private static final int PN = 4;
    private static final int SNOT = 30;

    private static final String AVHR_FILENAME =
            "AVHR_x1x_1B_M02_20070717013103Z_20070717031303Z_N_O_20070717031613Z.nat";
    private static final String IASI_FILEPATH =
            "src/test/data/IASI_xxx_1C_M02_20071201092659Z_20071201110554Z_N_O_20071201111153Z.nat";

    public void testReadSpectrum() throws IOException {
        final int[] sampleIndexes = new int[]{0, 1, 2, 8460};
        final double[][] sampleRadiances = {
                {4.762E-4, 4.936E-4, 5.253E-4, 1.497E-6},
                {4.792E-4, 4.936E-4, 5.165E-4, 2.924E-6},
                {4.736E-4, 4.914E-4, 5.190E-4, 1.586E-6},
                {4.794E-4, 4.922E-4, 5.142E-4, 1.127E-6}};

        final IasiFile iasiFile = createIasiFile();
        final int ifovId = IasiFile.computeIfovId(7, 17, 0);

        for (int i = 0; i < PN; ++i) {
            final double[][] spectrum = iasiFile.readSpectrum(ifovId + i);
            assertEquals(8461, spectrum.length);

            for (int k = 0; k < 4; ++k) {
                assertEquals(sampleRadiances[i][k], spectrum[sampleIndexes[k]][1], 1.0E-12);
            }
        }

        iasiFile.close();
    }

    public void testReadGEPSIasiMode() throws IOException {
        final IasiFile iasiFile = createIasiFile();
        final byte[] modes = iasiFile.readGEPSIasiMode();

        assertEquals(9, modes.length);

        for (final byte mode : modes) {
            assertEquals(0, mode);
        }

        iasiFile.close();
    }

    public void testReadGQisFlagQual() throws IOException {
        final IasiFile iasiFile = createIasiFile();
        final boolean[][][] qualityFlags = iasiFile.readGQisFlagQual();

        assertEquals(9, qualityFlags.length);
        assertEquals(SNOT, qualityFlags[0].length);
        assertEquals(PN, qualityFlags[0][0].length);

        for (final boolean[][] mdrQualityFlags : qualityFlags) {
            for (final boolean[] efovQualityFlags : mdrQualityFlags) {
                for (boolean anomalous : efovQualityFlags) {
                    assertFalse(anomalous);
                }
            }
        }

        iasiFile.close();
    }

    public void testIndexComputation() {
        assertIndexComputationCorrectness(0, 0, 0);
        assertIndexComputationCorrectness(0, 0, 1);
        assertIndexComputationCorrectness(0, 0, 2);
        assertIndexComputationCorrectness(0, 0, 3);

        assertIndexComputationCorrectness(4, 7, 1);
        assertIndexComputationCorrectness(4, 7, 1);
    }

    public void assertIndexComputationCorrectness(int mdrIndex, int efovIndex, int ifovIndex) {
        final int ifovId = IasiFile.computeIfovId(mdrIndex, efovIndex, ifovIndex);
        assertEquals((mdrIndex * SNOT + efovIndex) * PN + ifovIndex, ifovId);

        assertEquals(mdrIndex, IasiFile.computeMdrIndex(ifovId));
        assertEquals(efovIndex, IasiFile.computeEfovIndex(ifovId));
        assertEquals(ifovIndex, IasiFile.computeIfovIndex(ifovId));
    }

    public void testIasiFilenameFilter() {
        final IasiFile.IasiFilenameFilter filter = new IasiFile.IasiFilenameFilter(AVHR_FILENAME);

        assertTrue(filter.accept(null, "IASI_x1x_1C_M02_20070717012957Z_20070717031156Z_N_O_20070717031940Z.nat"));
        assertTrue(filter.accept(null, "IASI_x1x_1C_M02.nat"));

        assertFalse(filter.accept(null, "AVHR_x1x_1C_M02.nat"));
        assertFalse(filter.accept(null, "IASI_x1x_1B_M02.nat"));
        assertFalse(filter.accept(null, "IASI_x1x_1C_M03.nat"));
        assertFalse(filter.accept(null, "IASI_x1x_1C_M02_20070717012957Z_20070717031156Z_N_O_20070717031940Z.hdf"));
        assertFalse(filter.accept(null, "IASI_x2x_1C_M02_20070717012957Z_20070717031156Z_N_O_20070717031940Z.nat"));
    }

    public void testExtractTime() throws ParseException {
        try {
            IasiFile.extractStartTimeInMillis("AVHR_x1x_1B_M02_A00707170");
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            IasiFile.extractStartTimeInMillis("AVHR_x1x_1B_M02_A0070717013103Z_20070717031303Z_N_O_20070717031613Z.nat");
            fail();
        } catch (ParseException expected) {
        }

        final long millis = IasiFile.extractStartTimeInMillis(AVHR_FILENAME);
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear(Calendar.MILLISECOND);
        calendar.set(2007, 6, 17, 1, 31, 3);

        assertEquals(calendar.getTimeInMillis(), millis);
    }

    public void testFindIasiFile() throws ParseException {
        final File[] iasiFiles = {
                new File("IASI_x1x_1C_M02_20070702014152Z_20070702032352Z_N_O_20070702032834Z.nat"),
                new File("IASI_x1x_1C_M02_20070615023300Z_20070615041755Z_N_O_20070615042035Z.nat"),
                new File("IASI_x1x_1C_M02_20070704143000Z_20070704160855Z_N_O_20070704161442Z.nat"),
                new File("IASI_x1x_1C_M02_20070628012353Z_20070628030553Z_N_O_20070628031110Z.nat"),
        };

        final String avhrrFileName = "AVHR_x1x_1B_M02_20070615023403Z_20070615041903Z_N_O_20070615041730Z.nat";
        final long avhrrStartTime = IasiFile.extractStartTimeInMillis(avhrrFileName);
        final File iasiFile = EpsFile.findFile(avhrrStartTime, iasiFiles);

        assertEquals(iasiFiles[1], iasiFile);
        assertNull(EpsFile.findFile(avhrrStartTime, new File[0]));
    }

    public static IasiFile createIasiFile() {
        try {
            final long avhrrStartMillis = ProductData.UTC.parse("01-DEC-2007 09:57:59.969").getAsCalendar().getTimeInMillis();
            final long avhrrEndMillis = ProductData.UTC.parse("01-DEC-2007 09:59:00.136").getAsCalendar().getTimeInMillis();

            final int avhrrRasterHeight = 361;
            final int avhrrTrimX = 4;

            EpsFile openFile = EpsFormats.getInstance().openFile(new File(IASI_FILEPATH));
            return (IasiFile) openFile;
//            return new IasiFile(, avhrrStartMillis, avhrrEndMillis, avhrrRasterHeight, avhrrTrimX);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            fail();
            return null;
        }
    }
}
