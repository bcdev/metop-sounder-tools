/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package org.eumetsat.iasi.dataio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.stream.FileImageInputStream;

import org.esa.beam.framework.datamodel.ProductData;

public class IasiL2File {

    private MainProductHeaderRecord mainProductHeaderRecord;
    private FileImageInputStream iis;
    private long firstMdrOffset;
    private int mdrCount;
    private long[] mdrStartOffset;

    public IasiL2File(File file) {
        try {
            iis = new FileImageInputStream(file);
            readHeader();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void readHeader() throws IOException {
        final GenericRecordHeader mphrHeader = GenericRecordHeader.readGenericRecordHeader(iis);

        if (mphrHeader.recordClass != RecordClass.MPHR
                || mphrHeader.instrumentGroup != InstrumentGroup.GENERIC
                || mphrHeader.recordSubclass != 0) {
            throw new IOException("Illegal Main Product Header Record");
        }

        mainProductHeaderRecord = new MainProductHeaderRecord();
        mainProductHeaderRecord.readRecord(iis);
        mdrCount = mainProductHeaderRecord.getIntValue("TOTAL_MDR");

        final List<InternalPointerRecord> iprList = new ArrayList<InternalPointerRecord>();
        for (; ;) {
            final InternalPointerRecord ipr = InternalPointerRecord.readInternalPointerRecord(iis);
            iprList.add(ipr);
            if (ipr.targetRecordClass == RecordClass.MDR) {
                break;
            }
        }

        for (final InternalPointerRecord ipr : iprList) {
            if (ipr.targetRecordClass == RecordClass.MDR) {
                firstMdrOffset = ipr.targetRecordOffset;
            }
        }
        mdrStartOffset = new long[mdrCount];

        long pos = firstMdrOffset;
        synchronized (this.iis) {
            for (int i = 0; i < mdrCount; i++) {
                mdrStartOffset[i] = pos;
                iis.seek(pos);
                GenericRecordHeader mdrHeader = GenericRecordHeader
                        .readGenericRecordHeader(iis);
                pos += mdrHeader.recordSize;
            }
        }
    }
    
    public int getMdrCount() {
        return mdrCount;
    }
    
    public CloudDataContainer readCloudData(int mdrIndex) throws IOException {
        final ProductData flg_cldtst = readCloudFlagByte(mdrIndex, 77921);
        final ProductData flg_iasicld = readCloudFlagByte(mdrIndex, 79241);
        final ProductData flg_iasiclr = readCloudFlagByte(mdrIndex, 79361);
        final ProductData flg_iasibad = readCloudFlagShort(mdrIndex, 79001);
        final ProductData flg_landsea = readCloudFlagByte(mdrIndex, 79841);
        int[] latLon = readLatLon(mdrIndex);
        double[] lon = new double[120];
        double[] lat = new double[120];
        int latLonIndex = 0;
        for (int i = 0; i < lat.length; i++) {
            lat[i] = latLon[latLonIndex++] / 1E4;
            lon[i] = latLon[latLonIndex++] / 1E4;
        }
        
        return new CloudDataContainer(flg_cldtst, flg_iasicld, flg_iasiclr, flg_iasibad, flg_landsea, lat, lon);
    }
    
    private ProductData readCloudFlagByte(int mdrIndex, int offset) throws IOException {
        byte[] flg = new byte[120];
        synchronized (iis) {
            iis.seek(mdrStartOffset[mdrIndex] + offset);
            iis.read(flg);
        }
        return ProductData.createUnsignedInstance(flg);
    }
    
    private ProductData readCloudFlagShort(int mdrIndex, int offset) throws IOException {
        short[] flg = new short[120];
        synchronized (iis) {
            iis.seek(mdrStartOffset[mdrIndex] + offset);
            iis.readFully(flg, 0, flg.length);
        }
        return ProductData.createInstance(flg);
    }
    
    private int[] readLatLon(int mdrIndex) throws IOException {
        int[] latLon = new int[240];
        synchronized (iis) {
            iis.seek(mdrStartOffset[mdrIndex] + 75281);
            iis.readFully(latLon, 0, latLon.length);
        }
        return latLon;
    }
    
    public static class CloudDataContainer{

        public final ProductData flg_cldtst;
        public final ProductData flg_iasicld;
        public final ProductData flg_iasiclr;
        public final ProductData flg_iasibad;
        public final double[] lat;
        public final double[] lon;
        public final ProductData flg_landsea;

        public CloudDataContainer(ProductData flg_cldtst,
                ProductData flg_iasicld, ProductData flg_iasiclr,
                ProductData flg_iasibad, ProductData flg_landsea, double[] lat, double[] lon) {
                    this.flg_cldtst = flg_cldtst;
                    this.flg_iasicld = flg_iasicld;
                    this.flg_iasiclr = flg_iasiclr;
                    this.flg_iasibad = flg_iasibad;
                    this.flg_landsea = flg_landsea;
                    this.lat = lat;
                    this.lon = lon;
        }
    }

}
