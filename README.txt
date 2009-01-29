
                                  README

                        IAVISA Tools, Version 1.2.2
                             20. October 2008


Introduction
============
The VISAT IAVISA Tools are used to select IASI IFOVs on an subjacent AVHRR data
products and to add, edit and remove them from the IAVISA database.
The tools are developed to enable an expert on visual scene analysis to
efficiently collect thousands of samples.
Each sample classifies a single IASI instantaneous field of view (IFOV) as
being cloud-free (clear sky), Partly cloudy low, Partly cloudy high and cloudy.


Installation
============
The IAVISA Tools require an installed BEAM (version 4.2).
A 4.2-SNAPSHOT version can be retrieved from the BEAM Wiki
(http://www.brockmann-consult.de/beam-wiki/x/DYAN).
Installers for Windows and Linux of the current development snapshot are
also provided with this delivery.
After official release the BEAM installer can be downloaded from the
BEAM website (http://www.brockmann-consult.de/beam/).

Unpack iavisa-<version>.zip directly into your BEAM-4.2 installation directory.
The directory structure of the zip-file is aligned to the structure of the
BEAM-4.2 installation directory.


Configuration
=============
When installation is done, open the file ${BEAM_HOME}/config/iavisa.config
in a text editor.

Using an existing database
~~~~~~~~~~~~~~~~~~~~~~~~~~
To use an existing database you have to change "iavisa.database.url" parameter
to the location where the database is located, e.g.

Windows :  iavisa.database.url = jdbc:hsqldb:file:c:\\iavisa\database\\initial-dataset
Linux   :  iavisa.database.url = jdbc:hsqldb:file:/usr/local/iavisa/database/initial-dataset

PLEASE NOTE: The path has to be an absolute path.

The last part of the url denotes the name of the database.
A database is comprised of at least four files in one directory:
    - database
     |--- initial-dataset.lck
     |--- initial-dataset.log
     |--- initial-dataset.properties
     |--- initial-dataset.script

Also you have to ensure, that the "iavisa.database.runInitScript"
parameter is set to "false", which is by default.

All other parameters are breifly explained directly in
the configuration file.

IAVISA Analyser Tool
~~~~~~~~~~~~~~~~~~~~
The IAVISA Analyser Tool can be used directly from within VISAT.
If you like to use it in stand-alone mode you have to set the
BEAM4_HOME variable in ${BEAM_HOME}/bin/iavisa-analyser.sh on Unix,
and iavisa-analyser.bat on Windows accordingly to the BEAM-4.2 installation
directory.



=============================================================================
Software User Manual
=============================================================================

Introduction to the VISAT IAVISA Tools
=====================================

After the configuration is completed, you can now run VISAT.

After VISAT ist started, try the following:

1. In the main menu, select
    "File/Import/Import MetOp-AVHRR/3 Level-1b Product (or Subset)..."
2. Open a AVHRR Data Product (not a subset!) and open a greyscale or
   RGB image. The corresponding IASI product will be automatically loaded if it
   resides in the same directory as MetOp-AVHRR product. If it can not be
   found, you will be asked to select it manually.
3. In the main tool bar, select the "IASI Sample Collection Tool". This is the
   one with the icon showing a pipette over a cloud. The sample classification
   window pops up and if you move the mouse over the image the classification
   categories will automatically update. Note that you can change the behaviour
   of the update process. If in the sample classification window the check box
   "Use IFOV center" is selected the categories are updated for the center
   pixel of the IFOV next to the mouse position. If you unselect this check box
   the categories are shown for pixel directly under the mouse cursor.
4. Click on an IFOV, the color changes from red to green, this means that the
   IFOV is the currently selected one. For further information about
   the IFOV you can invoke the "IASI Info View" by clicking the button in the
   menu bar with icon showing an 'i' with the IASI symbol. This view shows you
   information about the location and the geometry, the IASI spectrum and the
   radiance analysis of the IFOV.
5. Now collect some IFOV samples. When clicking on an IFOV hold down the
   CTRL key. The IFOV is added to the database. If an IFOV is already in the
   database you can easily remove it by pressing the 'ALT' key when clicking
   on it.
5. In the main menu, select "View/Tool Windows/IAVISA - Sample Records".
   The sample window pops up. It lists all collected samples of the current
   product. If you select entries in this list, samples will be hilighted in
   the image accordingly.
   o Use "Commit" to store the samples persistently in the database.
   o Use "Rollback" to re-load samples from the database and undo your changes
     since last commit.
   o Use "Delete" to delete the selected samples permanently from the
     database


Notes regarding the MetOp AVHRR/3 Level-1b Reader
=================================================
Note that the MetOp-AVHRR reader only considers pixels of which geographical
positions can be determined without extrapolating the tie-point grids:
The nominal AVHRR scan line exhibits 2048 pixels with either 51 (low resolution)
or 103 (high resolution) tie points, which start at pixel number 25 or 5 and
are sampled at intervals of 40 and 20 pixels, respectively. All pixels before
the first and after the last tie-point in an image row are not considered by
the AVHRR reader.
So the pixel at [0,0] (zero-based, upper-left most) in a VISAT image
corresponds to the pixel at [24,0] , respectively at [4,0] in the original
AVHRR scene.


Introduction to the IAVISA Analyser Tool
========================================
The IAVISA Analyser tool is used to analyse the current state of the IAVISA
database. It runs independently from BEAM.
You can also use the IAVISA Analyser from within VISAT.
Go to "View/Tool Windows/IAVISA - Sample Records" or use the tool bar button,
the one with table and magnifier icon.


Fixing Problems
===============

o If you get any database errors during startup, check the database
  configuration.
  For example, if you get error:
       "table not found: SAMPLE_ATTRIBUTE_TYPE in statement [SELECT..."
  check the configuration property "database.url" in the file
  ./avisa-config.properties". If the value starts with "jdbc:hsqldb:file:"
  then ensure that the following path is valid for your local filesystem.
o If you use the local file database and you get the error
       "database already in use by another process"
  quit all IAVISA tools restart either VISAT or the IAVISA Analyser Tool. Both
  tools cannot be run at the same time when a local file database is used.


  For further support please contact
        norman.fomferra@brockmann-consult.de
     or marco.peters@brockmann-consult.de

    Geesthacht, 20. October 2008