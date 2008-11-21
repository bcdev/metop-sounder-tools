package org.eumetsat.iasi.dataio;

import com.bc.ceres.binio.*;
import com.bc.ceres.binio.util.DataPrinter;
import com.bc.ceres.binio.util.ImageIOHandler;
import com.bc.ceres.binio.util.SequenceElementCountResolver;
import static com.bc.ceres.binio.util.TypeBuilder.*;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Formats for cloud test auxiliary data.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class AuxiliaryDataFormats {

    /**
     * Auxiliary data format for cloud test A.
     */
    public static final Format IASI_L2_PGS_THR_CLDDET_FORMAT;

    /**
     * Auxiliary data format for cloud test B.
     */
    public static final Format IASI_L2_PGS_COF_IASINT_FORMAT;

    /**
     * Auxiliary data format for cloud test D.
     */
    public static final Format IASI_L2_PGS_THR_HORCO_FORMAT;

    /**
     * Auxiliary data format for cloud test E.
     */
    public static final Format IASI_L2_PGS_THR_EOFRES_FORMAT;

    /**
     * Auxiliary data format for cloud test F.
     */
    public static final Format IASI_L2_PGS_THR_WINCOR_FORMAT;

    /**
     * Auxiliary data format for cloud test G.
     */
    public static final Format IASI_L2_PGS_THR_POLCLD_FORMAT;

    /**
     * Auxiliary data format for cloud test H.
     */
    public static final Format IASI_L2_PGS_THR_DESSTR_FORMAT;

    public static final String CHANNEL_IDENTIFICATION_NUMBERS = "Channel_Identification_Numbers";
    public static final String CLEAR_SKY_EOFS = "Clear_Sky_Eofs";
    public static final String COEFFICIENTS_A = "Coefficients_A";
    public static final String COEFFICIENTS_B = "Coefficients_B";
    public static final String COEFFICIENTS_C = "Coefficients_C";
    public static final String DESERT_IDENTIFICATION_MAP = "Desert_Identification_Map";
    public static final String DIFFERENCE_VALUES = "Difference_Values";
    public static final String LATITUDE_IDENTIFIERS = "Latitude_Identifiers";
    public static final String LATITUDE_LONGITUDE_GRID_POINTS = "Latitude_Longitude_Grid_Points";
    public static final String LATITUDE_ZONE_IDENTIFIERS = "Latitude_Zone_Identifiers";
    public static final String MAP_OF_ELEVATED_POLAR_REGIONS = "Map_Of_Elevated_Polar_Regions";
    public static final String MONTH_IDENTIFIERS = "Month_Identifiers";
    public static final String NUMBER_OF_CHANNELS = "Number_Of_Channels";
    public static final String NUMBER_OF_CLEAR_SKY_EOFS = "Number_Of_Clear_Sky_Eofs";
    public static final String NUMBER_OF_LATITUDE_BANDS = "Number_Of_Latitude_Bands";
    public static final String NUMBER_OF_LATITUDE_ZONES = "Number_Of_Latitude_Zones";
    public static final String NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS = "Number_Of_Latitude_Longitude_Grid_Points";
    public static final String NUMBER_OF_MONTHS = "Number_Of_Months";
    public static final String NUMBER_OF_SPECTRAL_LAGS = "Number_Of_Spectral_Lags";
    public static final String NUMBER_OF_SURFACE_CONDITIONS = "Number_Of_Surface_Conditions";
    public static final String NUMBER_OF_SURFACE_IDENTIFIERS = "Number_Of_Surface_Identifiers";
    public static final String REFERENCE_SPECTRA = "Reference_Spectra";
    public static final String SURFACE_IDENTIFIERS = "Surface_Identifiers";
    public static final String THRESHOLD_VALUES = "Threshold_Values";

    private static final CompoundType GEO_POS_TYPE = COMP("GEO_POS_TYPE",
                                                          MEMBER("Latitude", DOUBLE), MEMBER("Longitude", DOUBLE));


    /////// Initialization of IASI_L2_PGS_THR_CLDDET_FORMAT /////////
    static {
        final CompoundType clddetType = COMP("IASI_L2_PGS_THR_CLDDET_TYPE",
                                             MEMBER(NUMBER_OF_CHANNELS, LONG),
                                             MEMBER(NUMBER_OF_LATITUDE_BANDS, LONG),
                                             MEMBER(NUMBER_OF_MONTHS, LONG),
                                             MEMBER(LATITUDE_IDENTIFIERS, SEQ(DOUBLE)),
                                             MEMBER(MONTH_IDENTIFIERS, SEQ(LONG)),
                                             MEMBER(CHANNEL_IDENTIFICATION_NUMBERS, SEQ(LONG)),
                                             MEMBER(THRESHOLD_VALUES, SEQ(DOUBLE)),
                                             MEMBER(DIFFERENCE_VALUES, SEQ(DOUBLE))
        );
        IASI_L2_PGS_THR_CLDDET_FORMAT = new Format(clddetType, ByteOrder.BIG_ENDIAN);

        IASI_L2_PGS_THR_CLDDET_FORMAT.addSequenceElementCountResolver(clddetType, LATITUDE_IDENTIFIERS,
                                                                      NUMBER_OF_LATITUDE_BANDS);
        IASI_L2_PGS_THR_CLDDET_FORMAT.addSequenceElementCountResolver(clddetType, MONTH_IDENTIFIERS, NUMBER_OF_MONTHS);
        IASI_L2_PGS_THR_CLDDET_FORMAT.addSequenceElementCountResolver(clddetType, CHANNEL_IDENTIFICATION_NUMBERS,
                                                                      NUMBER_OF_CHANNELS);
        IASI_L2_PGS_THR_CLDDET_FORMAT.addSequenceTypeMapper(
                clddetType.getMember(6),
                new SequenceElementCountResolver() {
                    public int getElementCount(CollectionData collectionData,
                                               SequenceType sequenceType) throws IOException {
                        final int numChannels = collectionData.getInt(0);
                        final int numLatitudeBands = collectionData.getInt(1);
                        final int numMonths = collectionData.getInt(2);
                        return numChannels * numLatitudeBands * numMonths * 2;
                    }
                });

        IASI_L2_PGS_THR_CLDDET_FORMAT.addSequenceTypeMapper(
                clddetType.getMember(7),
                new SequenceElementCountResolver() {
                    public int getElementCount(CollectionData collectionData,
                                               SequenceType sequenceType) throws IOException {
                        final int numChannels = collectionData.getInt(0);
                        final int numLatitudeBands = collectionData.getInt(1);
                        final int numMonths = collectionData.getInt(2);
                        return numChannels * numLatitudeBands * numMonths * 4;
                    }
                });
    }


    /////// Initialization of IASI_L2_PGS_COF_IASINT_FORMAT /////////
    static {
        final CompoundType iasintType =
                COMP("IASI_L2_PGS_COF_IASINT_TYPE",
                     MEMBER(NUMBER_OF_MONTHS, LONG),
                     MEMBER(MONTH_IDENTIFIERS, SEQ(LONG)),
                     MEMBER(NUMBER_OF_LATITUDE_ZONES, LONG),
                     MEMBER(LATITUDE_ZONE_IDENTIFIERS, SEQ(LONG)),
                     MEMBER(NUMBER_OF_SURFACE_CONDITIONS, SEQ(LONG, 2)),
                     MEMBER(CHANNEL_IDENTIFICATION_NUMBERS, SEQ(LONG, 16)),
                     MEMBER(THRESHOLD_VALUES, SEQ(DOUBLE)),
                     MEMBER(COEFFICIENTS_A, SEQ(DOUBLE)),
                     MEMBER(COEFFICIENTS_B, SEQ(DOUBLE)),
                     MEMBER(COEFFICIENTS_C, SEQ(DOUBLE))
                );

        IASI_L2_PGS_COF_IASINT_FORMAT = new Format(iasintType, ByteOrder.BIG_ENDIAN);

        IASI_L2_PGS_COF_IASINT_FORMAT.addSequenceElementCountResolver(iasintType,
                                                                      MONTH_IDENTIFIERS, NUMBER_OF_MONTHS);
        IASI_L2_PGS_COF_IASINT_FORMAT.addSequenceElementCountResolver(iasintType, LATITUDE_ZONE_IDENTIFIERS,
                                                                      NUMBER_OF_LATITUDE_ZONES);

        final SequenceTypeMapper elementCountResolver2 = new SequenceElementCountResolver() {
            public int getElementCount(CollectionData collectionData, SequenceType sequenceType) throws IOException {
                final long numMonths = collectionData.getLong(0);
                final long numLatitudeBands = collectionData.getLong(2);

                return (int) (numMonths * numLatitudeBands * 2 * 2);
            }
        };

        final SequenceTypeMapper elementCountResolver3 = new SequenceElementCountResolver() {
            public int getElementCount(CollectionData collectionData, SequenceType sequenceType) throws IOException {
                final long numMonths = collectionData.getLong(0);
                final long numLatitudeBands = collectionData.getLong(2);

                return (int) (numMonths * numLatitudeBands * 2 * 3);
            }
        };

        final SequenceTypeMapper elementCountResolver7 = new SequenceElementCountResolver() {
            public int getElementCount(CollectionData collectionData, SequenceType sequenceType) throws IOException {
                final long numMonths = collectionData.getLong(0);
                final long numLatitudeBands = collectionData.getLong(2);

                return (int) (numMonths * numLatitudeBands * 2 * 7);
            }
        };

        IASI_L2_PGS_COF_IASINT_FORMAT.addSequenceTypeMapper(iasintType.getMember(6), elementCountResolver3);
        IASI_L2_PGS_COF_IASINT_FORMAT.addSequenceTypeMapper(iasintType.getMember(7), elementCountResolver7);
        IASI_L2_PGS_COF_IASINT_FORMAT.addSequenceTypeMapper(iasintType.getMember(8), elementCountResolver7);
        IASI_L2_PGS_COF_IASINT_FORMAT.addSequenceTypeMapper(iasintType.getMember(9), elementCountResolver2);
    }

    /////// Initialization of IASI_L2_PGS_THR_HORCO_FORMAT /////////
    static {
        final CompoundType horcoType =
                COMP("IASI_L2_PGS_THR_HORCO_TYPE",
                     MEMBER(CHANNEL_IDENTIFICATION_NUMBERS, SEQ(LONG, 1)),
                     MEMBER(NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS, LONG),
                     MEMBER(LATITUDE_LONGITUDE_GRID_POINTS, SEQ(GEO_POS_TYPE)),
                     MEMBER(NUMBER_OF_MONTHS, LONG),
                     MEMBER(MONTH_IDENTIFIERS, SEQ(LONG)),
                     MEMBER(THRESHOLD_VALUES, SEQ(DOUBLE))
                );

        IASI_L2_PGS_THR_HORCO_FORMAT = new Format(horcoType, ByteOrder.BIG_ENDIAN);

        IASI_L2_PGS_THR_HORCO_FORMAT.addSequenceElementCountResolver(horcoType,
                                                                     MONTH_IDENTIFIERS, NUMBER_OF_MONTHS);
        IASI_L2_PGS_THR_HORCO_FORMAT.addSequenceElementCountResolver(horcoType, LATITUDE_LONGITUDE_GRID_POINTS,
                                                                     NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS);

        final SequenceTypeMapper elementCountResolver = new SequenceElementCountResolver() {
            public int getElementCount(CollectionData collectionData, SequenceType sequenceType) throws IOException {
                final long numLatitudeLongitudeBins = collectionData.getLong(1);
                final long numMonths = collectionData.getLong(3);

                return (int) (numMonths * numLatitudeLongitudeBins * 3);
            }
        };

        IASI_L2_PGS_THR_HORCO_FORMAT.addSequenceTypeMapper(horcoType.getMember(5), elementCountResolver);
    }


    /////// Initialization of IASI_L2_PGS_THR_EOFRES_FORMAT /////////
    static {
        final CompoundType desstrType = COMP("IASI_L2_PGS_THR_EOFRES_TYPE",
                                             MEMBER(NUMBER_OF_CHANNELS, LONG),
                                             MEMBER(NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS, LONG),
                                             MEMBER(NUMBER_OF_MONTHS, LONG),
                                             MEMBER(NUMBER_OF_SURFACE_IDENTIFIERS, LONG),
                                             MEMBER(CHANNEL_IDENTIFICATION_NUMBERS, SEQ(LONG)),
                                             MEMBER(LATITUDE_LONGITUDE_GRID_POINTS, SEQ(GEO_POS_TYPE)),
                                             MEMBER(MONTH_IDENTIFIERS, SEQ(LONG)),
                                             MEMBER(SURFACE_IDENTIFIERS, SEQ(LONG)),
                                             MEMBER(NUMBER_OF_CLEAR_SKY_EOFS, LONG),
                                             MEMBER(CLEAR_SKY_EOFS, SEQ(DOUBLE)),
                                             MEMBER(THRESHOLD_VALUES, SEQ(DOUBLE))
        );
        IASI_L2_PGS_THR_EOFRES_FORMAT = new Format(desstrType, ByteOrder.BIG_ENDIAN);
        IASI_L2_PGS_THR_EOFRES_FORMAT.addSequenceElementCountResolver(desstrType,
                                                                      CHANNEL_IDENTIFICATION_NUMBERS,
                                                                      NUMBER_OF_CHANNELS);
        IASI_L2_PGS_THR_EOFRES_FORMAT.addSequenceElementCountResolver(desstrType,
                                                                      LATITUDE_LONGITUDE_GRID_POINTS,
                                                                      NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS);
        IASI_L2_PGS_THR_EOFRES_FORMAT.addSequenceElementCountResolver(desstrType,
                                                                      MONTH_IDENTIFIERS, NUMBER_OF_MONTHS);
        IASI_L2_PGS_THR_EOFRES_FORMAT.addSequenceElementCountResolver(desstrType,
                                                                      SURFACE_IDENTIFIERS,
                                                                      NUMBER_OF_SURFACE_IDENTIFIERS);
        IASI_L2_PGS_THR_EOFRES_FORMAT.addSequenceTypeMapper(desstrType.getMember(9),
                                                            new SequenceElementCountResolver() {
                                                                public int getElementCount(
                                                                        CollectionData collectionData,
                                                                        SequenceType sequenceType) throws IOException {
                                                                    final int numChannels = collectionData.getInt(0);
                                                                    final int numClearSkyEofs = collectionData.getInt(
                                                                            8);
                                                                    return numChannels * numClearSkyEofs;
                                                                }

                                                            });
        IASI_L2_PGS_THR_EOFRES_FORMAT.addSequenceTypeMapper(desstrType.getMember(10),
                                                            new SequenceElementCountResolver() {
                                                                public int getElementCount(
                                                                        CollectionData collectionData,
                                                                        SequenceType sequenceType) throws IOException {
                                                                    final int numLatLonBins = collectionData.getInt(1);
                                                                    final int numMonth = collectionData.getInt(2);
                                                                    final int numSurfaceIdentifiers = collectionData.getInt(
                                                                            3);
                                                                    return numLatLonBins * numSurfaceIdentifiers * numMonth;
                                                                }
                                                            });
    }

    /////// Initialization of IASI_L2_PGS_THR_WINCOR_FORMAT /////////
    static {
        final CompoundType wincorType =
                COMP("IASI_L2_PGS_THR_WINCOR_TYPE",
                     MEMBER(NUMBER_OF_MONTHS, LONG),
                     MEMBER(MONTH_IDENTIFIERS, SEQ(LONG)),
                     MEMBER(NUMBER_OF_CHANNELS, LONG),
                     MEMBER(CHANNEL_IDENTIFICATION_NUMBERS, SEQ(LONG)),
                     MEMBER(NUMBER_OF_SPECTRAL_LAGS, LONG),
                     MEMBER(NUMBER_OF_LATITUDE_ZONES, LONG),
                     MEMBER(LATITUDE_IDENTIFIERS, SEQ(LONG)),
                     MEMBER(NUMBER_OF_SURFACE_IDENTIFIERS, LONG),
                     MEMBER(SURFACE_IDENTIFIERS, SEQ(LONG)),
                     MEMBER(REFERENCE_SPECTRA, SEQ(DOUBLE)),
                     MEMBER(THRESHOLD_VALUES, SEQ(DOUBLE))
                );

        IASI_L2_PGS_THR_WINCOR_FORMAT = new Format(wincorType, ByteOrder.BIG_ENDIAN);

        IASI_L2_PGS_THR_WINCOR_FORMAT.addSequenceElementCountResolver(wincorType,
                                                                      MONTH_IDENTIFIERS, NUMBER_OF_MONTHS);
        IASI_L2_PGS_THR_WINCOR_FORMAT.addSequenceElementCountResolver(wincorType, CHANNEL_IDENTIFICATION_NUMBERS,
                                                                      NUMBER_OF_CHANNELS);
        IASI_L2_PGS_THR_WINCOR_FORMAT.addSequenceElementCountResolver(wincorType, LATITUDE_IDENTIFIERS,
                                                                      NUMBER_OF_LATITUDE_ZONES);
        IASI_L2_PGS_THR_WINCOR_FORMAT.addSequenceElementCountResolver(wincorType, SURFACE_IDENTIFIERS,
                                                                      NUMBER_OF_SURFACE_IDENTIFIERS);

        final SequenceTypeMapper referenceSpectraCountResolver = new SequenceElementCountResolver() {
            public int getElementCount(CollectionData collectionData, SequenceType sequenceType) throws IOException {
                final long numMonths = collectionData.getLong(0);
                final long numChannels = collectionData.getLong(2);
                final long numLatitudeBands = collectionData.getLong(5);
                final long numSurfaceIdentifiers = collectionData.getLong(7);

                return (int) (numChannels * numMonths * numLatitudeBands * numSurfaceIdentifiers);
            }
        };

        final SequenceTypeMapper thresholdValueCountResolver = new SequenceElementCountResolver() {
            public int getElementCount(CollectionData collectionData, SequenceType sequenceType) throws IOException {
                final long numMonths = collectionData.getLong(0);
                final long numLatitudeBands = collectionData.getLong(5);
                final long numSurfaceIdentifiers = collectionData.getLong(7);

                return (int) (numMonths * numLatitudeBands * numSurfaceIdentifiers);
            }
        };

        IASI_L2_PGS_THR_WINCOR_FORMAT.addSequenceTypeMapper(wincorType.getMember(9), referenceSpectraCountResolver);
        IASI_L2_PGS_THR_WINCOR_FORMAT.addSequenceTypeMapper(wincorType.getMember(10), thresholdValueCountResolver);
    }

    /////// Initialization of IASI_L2_PGS_THR_POLCLD_FORMAT /////////
    static {

        final CompoundType polcldType =
                COMP("IASI_L2_PGS_THR_POLCLD_TYPE",
                     MEMBER(CHANNEL_IDENTIFICATION_NUMBERS, SEQ(LONG, 2)),
                     MEMBER(NUMBER_OF_MONTHS, LONG),
                     MEMBER(MONTH_IDENTIFIERS, SEQ(LONG)),
                     MEMBER(NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS, LONG),
                     MEMBER(LATITUDE_LONGITUDE_GRID_POINTS, SEQ(GEO_POS_TYPE)),
                     MEMBER(MAP_OF_ELEVATED_POLAR_REGIONS, SEQ(LONG)),
                     MEMBER(THRESHOLD_VALUES, SEQ(DOUBLE))
                );
        IASI_L2_PGS_THR_POLCLD_FORMAT = new Format(polcldType, ByteOrder.BIG_ENDIAN);

        IASI_L2_PGS_THR_POLCLD_FORMAT.addSequenceElementCountResolver(polcldType, MONTH_IDENTIFIERS, NUMBER_OF_MONTHS);
        IASI_L2_PGS_THR_POLCLD_FORMAT.addSequenceElementCountResolver(polcldType, LATITUDE_LONGITUDE_GRID_POINTS,
                                                                      NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS);
        IASI_L2_PGS_THR_POLCLD_FORMAT.addSequenceElementCountResolver(polcldType, MAP_OF_ELEVATED_POLAR_REGIONS,
                                                                      NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS);
        IASI_L2_PGS_THR_POLCLD_FORMAT.addSequenceElementCountResolver(polcldType, THRESHOLD_VALUES, NUMBER_OF_MONTHS);
    }

    /////// Initialization of IASI_L2_PGS_THR_DESSTR_FORMAT /////////
    static {
        final CompoundType desstrType = COMP("IASI_L2_PGS_THR_DESSTR_TYPE",
                                             MEMBER(NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS, LONG),
                                             MEMBER(NUMBER_OF_MONTHS, LONG),
                                             MEMBER(LATITUDE_LONGITUDE_GRID_POINTS, SEQ(GEO_POS_TYPE)),
                                             MEMBER(MONTH_IDENTIFIERS, SEQ(LONG)),
                                             MEMBER(CHANNEL_IDENTIFICATION_NUMBERS, SEQ(LONG, 2)),
                                             MEMBER(DESERT_IDENTIFICATION_MAP, SEQ(LONG)),
                                             MEMBER(THRESHOLD_VALUES, SEQ(DOUBLE))
        );
        IASI_L2_PGS_THR_DESSTR_FORMAT = new Format(desstrType, ByteOrder.BIG_ENDIAN);
        IASI_L2_PGS_THR_DESSTR_FORMAT.addSequenceElementCountResolver(desstrType,
                                                                      LATITUDE_LONGITUDE_GRID_POINTS,
                                                                      NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS);
        IASI_L2_PGS_THR_DESSTR_FORMAT.addSequenceElementCountResolver(desstrType,
                                                                      MONTH_IDENTIFIERS, NUMBER_OF_MONTHS);
        IASI_L2_PGS_THR_DESSTR_FORMAT.addSequenceElementCountResolver(desstrType,
                                                                      DESERT_IDENTIFICATION_MAP,
                                                                      NUMBER_OF_LATITUDE_LONGITUDE_GRID_POINTS);
        IASI_L2_PGS_THR_DESSTR_FORMAT.addSequenceTypeMapper(desstrType.getMember(6),
                                                            new SequenceElementCountResolver() {
                                                                public int getElementCount(
                                                                        CollectionData collectionData,
                                                                        SequenceType sequenceType) throws IOException {
                                                                    final int numGridPoints = collectionData.getInt(0);
                                                                    final int numMonths = collectionData.getInt(1);
                                                                    return numGridPoints * numMonths * 2;
                                                                }
                                                            });
    }


    /**
     * Prints out all the auxillary data formats
     *
     * @param args ignored
     *
     * @throws IOException when an IO error occurrs
     */
    public static void main(String[] args) throws IOException {
        List<IOContext> contextList = new ArrayList<IOContext>(7);
        final MemoryCacheImageInputStream clddetIis = new MemoryCacheImageInputStream(
                AuxiliaryDataFormats.class.getResourceAsStream("/auxdata/IASI_L2_PGS_THR_CLDDET"));
        contextList.add(new IOContext(IASI_L2_PGS_THR_CLDDET_FORMAT, new ImageIOHandler(clddetIis)));
        final MemoryCacheImageInputStream iasintIis = new MemoryCacheImageInputStream(
                AuxiliaryDataFormats.class.getResourceAsStream("/auxdata/IASI_L2_PGS_COF_IASINT"));
        contextList.add(new IOContext(IASI_L2_PGS_COF_IASINT_FORMAT, new ImageIOHandler(iasintIis)));
        final MemoryCacheImageInputStream horcoIis = new MemoryCacheImageInputStream(
                AuxiliaryDataFormats.class.getResourceAsStream("/auxdata/IASI_L2_PGS_THR_HORCO"));
        contextList.add(new IOContext(IASI_L2_PGS_THR_HORCO_FORMAT, new ImageIOHandler(horcoIis)));
        final MemoryCacheImageInputStream eofresIis = new MemoryCacheImageInputStream(
                AuxiliaryDataFormats.class.getResourceAsStream("/auxdata/IASI_L2_PGS_THR_EOFRES"));
        contextList.add(new IOContext(IASI_L2_PGS_THR_EOFRES_FORMAT, new ImageIOHandler(eofresIis)));
        final MemoryCacheImageInputStream wincorIis = new MemoryCacheImageInputStream(
                AuxiliaryDataFormats.class.getResourceAsStream("/auxdata/IASI_L2_PGS_THR_WINCOR"));
        contextList.add(new IOContext(IASI_L2_PGS_THR_WINCOR_FORMAT, new ImageIOHandler(wincorIis)));
        final MemoryCacheImageInputStream polcldIis = new MemoryCacheImageInputStream(
                AuxiliaryDataFormats.class.getResourceAsStream("/auxdata/IASI_L2_PGS_THR_POLCLD"));
        contextList.add(new IOContext(IASI_L2_PGS_THR_POLCLD_FORMAT, new ImageIOHandler(polcldIis)));
        final MemoryCacheImageInputStream desstrIis = new MemoryCacheImageInputStream(
                AuxiliaryDataFormats.class.getResourceAsStream("/auxdata/IASI_L2_PGS_THR_DESSTR"));
        contextList.add(new IOContext(IASI_L2_PGS_THR_DESSTR_FORMAT, new ImageIOHandler(desstrIis)));

        try {
            final DataPrinter dataPrinter = new DataPrinter(System.out, false);
            for (IOContext ioContext : contextList) {
                final CompoundData data = ioContext.getData();
                dataPrinter.print(data);
                System.out.println();
                System.out.println();
            }
        } finally {
            clddetIis.close();
            iasintIis.close();
            horcoIis.close();
            eofresIis.close();
            wincorIis.close();
            polcldIis.close();
            desstrIis.close();
        }
    }
}
