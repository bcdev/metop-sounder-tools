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
package org.eumetsat.metop.binio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.util.DataPrinter;


public class MetopFile {

    private final DataFormat format;
    private CompoundData metopData;

    public MetopFile(File file, DataFormat format) throws IOException {
        this.format = format;
        metopData = format.createContext(file, "r").getData();
    }
    
    public CompoundData getMetopData() {
        return metopData;
    }
    
    public static void main(String[] args) throws IOException {
        DataFormat metopFormat = MetopFormats.getInstance().getFormat();
        MetopFile metopFile = new MetopFile(new File(args[0]), metopFormat);
        DataPrinter printer = new DataPrinter();
        printer.print(metopFile.getMetopData());
    }
}
