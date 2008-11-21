package org.eumetsat.iasi.footprint;

import org.eumetsat.iasi.footprint.IasiFootprintLayerModel;
import org.eumetsat.iasi.dataio.IasiFile;
import org.esa.beam.framework.datamodel.Product;

public interface IasiFootprintLayerModelFactory {
    IasiFootprintLayerModel createLayerModel(Product avhrrProduct, IasiFile iasiFile);
}
