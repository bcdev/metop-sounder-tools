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
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URL;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.Format;
import com.bc.ceres.binio.IOContext;
import com.bc.ceres.binio.IOHandler;
import com.bc.ceres.binio.util.DataPrinter;
import com.bc.ceres.binio.util.RandomAccessFileIOHandler;

public class MetopReadMain {

    public static void main(String[] args) throws Exception {
        URL resource = MetopReadMain.class.getResource("eps_avhrrl1b_6.5.xml");
        File file = new File(args[0]);
        URI uri = resource.toURI();
        EpsXml epsXml = new EpsXml(uri);
        Format format = epsXml.getFormat();
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        final IOHandler handler = new RandomAccessFileIOHandler(raf);
        final IOContext context = new IOContext(format, handler);
        CompoundData metopData = context.getData();
        DataPrinter dataPrinter = new DataPrinter(System.out, false);
        dataPrinter.print(metopData);
    }
}
