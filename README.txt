
                                  README

                      Metop Sounder Tools, Version 1.0
                              29 January 2009


Introduction
============

The METOP Sounder Tools have been brought into life in order to support users
of data of the IASI, AMSU and MHS sensors on-board the EUMETSAT Metop satellites.

The software provides the following facilities for BEAM 4.5:

o Display of IASI, AMSU and MHS footprint overlays on-top of the corresponding
  AVHRR image
o Color manipulation of footprint overlays according to the brightness temperature
  measured for a selected sounder channel
o Display of IASI, AMSU and MHS brightness temperature spectra and meta data for a
  selected instantaneous field-of-view (IFOV)

The METOP Sounder Tools have been developed by Brockmann Consult under EUMETSAT
contract initiated and coordinated by Peter Schlüssel (EUMETSAT).


Installation
============

The Metop Sounder Tools require BEAM Version 4.5.1 or higher. BEAM installers for
different platforms can be downloaded from the BEAM website at

  http://www.brockmann-consult.de/beam/

For installing the Metop Sounder Tools unpack the file

  metop-sounder-tools-<version>.zip

directly into your BEAM installation directory. The directory structure of the
zip-file is adapted to the structure of the BEAM installation directory.

When there is a module 'beam-metop-avhrr-reader' of version 1.3 in the BEAM
modules directory, please remove the module manually. The Metop Sounder Tools
require at least Version 1.4 of the module.


Usage
=====

After VISAT has been launched, try the following:

1. In the VISAT main menu, use the file open dialog to open a Metop AVHRR
   Level-1b Product
2. Open a new product scene view for any band of the AVHRR product or create
   an RGB scene view
3. If residing in the same directory as the AVHRR product, the corresponding
   IASI, AMSU and MHS products will automatically be loaded and displayed as
   overlays on-top of the AVHRR image. The overlays also appear as additional
   layers in the VISAT layer manager, where the visibility and transparancy
   of individual layers can be changed
4. For inspecting individual sounder IFOVs activate any of the sounder tools
   arranged in the Metop toolbar - a group of three buttons with icons for
   IASI,AMSU and MHS
5. The usage of the Metop tools is further described by the Java help facility,
   which can be invoked by clicking on the help button (question mark) located
   in the lower right corner of each Metop tool


Support
=======

For further support please contact:

Norman Fomferra, norman.fomferra@brockmann-consult.de
Ralf Quast, ralf.quast@brockmann-consult.de
Marco Zühlke, marco.zuehlke@brockmann-consult.de

Brockmann Consult
Max-Planck-Straße 2
D-21502 Geesthacht
