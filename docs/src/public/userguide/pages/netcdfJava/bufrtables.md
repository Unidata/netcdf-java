---
title: The Common Data Model
last_updated: 2019-06-27
sidebar: netcdfJavaTutorial_sidebar 
permalink: bufr_tables.html
toc: false
---
## BUFR Table Handling in CDM

### Standard table mapping

A _standard table_ is a BUFR parameter table that is automatically used by the CDM. A _standard table map_ is an association of a standard table with a center/subcenter/version id. The CDM internally loads one table map located in <b>_bufr.jar_</b> at <b>/resources/bufrTables/local/tablelookup.csv_</b>. It is a csv (comma seperated value) format:

~~~
#
# BUFR TableLookup
#
# ids = Identification Section. bytes refer to editions 3 or 4. -1 means dont match
#
# name = for display only
# center = originating/generating centre  (ed3=ids bytes 6, ed4=ids byte 5,6)
# subcenter = originating/generating sub-centre  (ed3=ids byte 5, ed4=ids byte 7,8)
# master = Version number of master tables used (ed3=ids byte 11, ed4=ids byte 14)
# local = Version number of local tables used to augment the master table (ed3=ids byte 12, ed4=ids byte 15)
# cat = data category (ed3=ids byte 9, ed4=ids byte 11)
# tableB,tableD = file name or resource name
# format = ecmwf, mel_bufr, mel_tabs, ncep, ncep_nm, opera, ukmet, wmo_csv, wmo_xml
# mode = "wmoOnly", "wmoLocal", "localWmo"  (default "localWmo")
#
# name,center,subcenter,master,local,cat,tableB,tableBformat,tableD,tableDformat, mode
   WMO.07, 0,     0,        7,     0,    -1,   resource:/resources/bufrTables/cypher/B_d00v07.htm, cypher, resource:/resources/bufrTables/cypher/D_d00v07.htm, cypher
   WMO.08, 0,     0,        8,     0,    -1,   resource:/resources/bufrTables/cypher/B_d00v08.htm, cypher, resource:/resources/bufrTables/cypher/D_d00v08.htm, cypher
   WMO.09, 0,     0,        9,     0,    -1,   resource:/resources/bufrTables/cypher/B_d00v09.htm, cypher, resource:/resources/bufrTables/cypher/D_d00v09.htm, cypher
   WMO.10, 0,     0,        10,    0,    -1,   resource:/resources/bufrTables/cypher/B_d00v10.htm, cypher, resource:/resources/bufrTables/cypher/D_d00v10.htm, cypher
   WMO.11, 0,     0,        11,    0,    -1,   resource:/resources/bufrTables/cypher/B_d00v11.htm, cypher, resource:/resources/bufrTables/cypher/D_d00v11.htm, cypher
   WMO.12, 0,     0,        12,    0,    -1,   resource:/resources/bufrTables/cypher/B_d00v12.htm, cypher, resource:/resources/bufrTables/cypher/D_d00v12.htm, cypher
   WMO.13, 0,     0,        13,    0,    -1,   resource:/resources/bufrTables/cypher/B_d00v13.htm, cypher, resource:/resources/bufrTables/cypher/D_d00v13.htm, cypher
   WMO.14, 0,     0,        14,    0,    -1,   resource:/resources/bufrTables/wmo/BCTableB_BUFR14_2_0_CREX_6_2_0.xml, wmo_xml,resource:/resources/bufrTables/wmo/BTableD_BUFR14_2_0_CREX_6_2_0.xml, wmo_xml
   WMO.15, 0,     0,        15,    0,    -1,   resource:/resources/bufrTables/wmo/BUFR_15_1_1_TableB_E.xml, wmo_xml, resource:/resources/bufrTables/wmo/BUFR_15_1_1_TableD_E.xml, wmo_xml
   WMO.16, 0,     0,        16,    0,    -1,   resource:/resources/bufrTables/wmo/BUFRCREX_16_0_0_TableB_E.xml, wmo_xml, resource:/resources/bufrTables/wmo/BUFR_16_0_0_TableD_E.xml, wmo_xml
   WMO.any,0,     0,        -1,    0,    -1,   resource:/resources/bufrTables/cypher/B_d00v13.htm, cypher, resource:/resources/bufrTables/cypher/D_d00v13.htm, cypher
#
# NCEP
# see http://www.emc.ncep.noaa.gov/mmb/data_processing/bufrtab_tablea.htm#1
# also http://www.emc.ncep.noaa.gov/mmb/data_processing/bufrtab_tabled.htm
# also http://www.nco.ncep.noaa.gov/pmb/codes/nwprod/parm/
   NCEP.ETA, 7,    3,       -1,    -1,    241, resource:/resources/bufrTables/local/ncep/ncep.bufrtab.ETACLS1, ncep_nm, resource:/resources/bufrTables/local/ncep/ncep.bufrtab.ETACLS1, ncep_nm
   NCEP.NGM, 7,    3,       -1,    -1,    242, resource:/resources/bufrTables/local/ncep/ncep.bufrtab.NGMCLS1, ncep_nm, resource:/resources/bufrTables/local/ncep/ncep.bufrtab.NGMCLS1, ncep_nm
   NCEP.GFS, 7,    3,       -1,    -1,    243, resource:/resources/bufrTables/local/ncep/ncep.bufrtab.GFSCLS1, ncep_nm, resource:/resources/bufrTables/local/ncep/ncep.bufrtab.GFSCLS1, ncep_nm
   NCEP.12,  7,    8,       12,    -1,    -1, resource:/resources/bufrTables/local/ncep/ncepAwc.v12.override.csv, wmo_csv, , ,
   NCEP.14,  7,   -1,       14,    -1,    -1, resource:/resources/bufrTables/local/ncep/ncep.B07.14.local.csv, wmo_csv, resource:/resources/bufrTables/local/ncep/ncep.B4L-007-013-D.diff, mel_bufr
   NCEP.any, 7,   -1,       -1,    -1,    -1, resource:/resources/bufrTables/local/ncep/ncep.B07.local.csv, wmo_csv, resource:/resources/bufrTables/local/ncep/ncep.B4L-007-013-D.diff, mel_bufr
...
~~~ 

1. Each row contains the <b>_center id, subcenter id, master table version_</b>, and <b>_local table version_</b>. These are use to look for a match on the BUFR record. The first exact match is used.
2. If there is no exact match, then a wildcard match is used, where a "-1" matches any id. The first wildcard match is used.
3. When a match is made, the given path to a BUFR <b>_table B_</b> and a BUFR <b>table D_</b> is used. Various table formats are available, given by the <b>_format_</b>.
4. NCEP uses a matching scheme that may depend on the <b>_data category_</b>.
5. The mode has the following meaning:
    * <b>_wmoOnly_<b>: wmo entries (x < 48 && y < 192) can only be taken from wmo table (center = 0)
    * <b>_wmoLocal_</b>: if wmo entries not found in wmo table, look in local table
    * <b>_localWmo_</b>: look in local table first, then wmo table
    
###  Adding a user-defined standard table map
A user can programmatically add another lookup table by calling ucar.nc2.iosp.bufr.tables.BufrTables.addLookupFile(). It must be in the same csv format as above. It will be searched first.

This can also be done through the [runtime configuration](runtime_loading.html) XML file.

