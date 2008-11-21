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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.eumetsat.iasi.dataio.IasiL2File.CloudDataContainer;

public class IasiL2CloudExporter {

    public static void main(String[] args) {
        String filename = args[0];
        File file = new File(filename);
        PrintStream out = System.out;
        if (!file.exists()) {
            out.println("file does not exist: "+file.getAbsolutePath());
        }
        
        IasiL2File iasiL2File = new IasiL2File(file);
        int mdrCount = iasiL2File.getMdrCount();
        
        try {
            final String SEP = "\t";
            out = new PrintStream(new FileOutputStream("iasi_l2_export_2.csv"));
            out.println("mdrCount,index,flg_cldtst,flg_iasicld,flg_iasiclr,flg_iasibad,lat,lon");
            for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
                CloudDataContainer cloudDataContainer = iasiL2File.readCloudData(mdrIndex);
                for (int j = 0; j < cloudDataContainer.flg_cldtst.getNumElems(); j++) {
                    out.println(mdrIndex+SEP+j+SEP+
                            Integer.toBinaryString(cloudDataContainer.flg_cldtst.getElemIntAt(j))+SEP+
                            Integer.toBinaryString(cloudDataContainer.flg_iasicld.getElemIntAt(j))+SEP+
                            cloudDataContainer.flg_iasiclr.getElemIntAt(j)+SEP+
                            Integer.toBinaryString(cloudDataContainer.flg_iasibad.getElemIntAt(j))+SEP+
                            cloudDataContainer.lat[j]+SEP+
                            cloudDataContainer.lon[j]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
