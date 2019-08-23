These are the tables gatherd by Robb up until 2011.
below are his notes.
---------------------------------------------------------
            GRIB TABLE FILE NAMING

The following naming syntax for grib table
files (.tab) is used:

    <center>_<optional string><table-id>.tab

Example : In file 'ecmwf_128.tab',
    center = ECMWF ()
    table-id = 128

    Center and table-id must match the first
    line of tab file content:
        -1:98:-1:128,

    where center_id 98 matches center 'ECMWF'
        (see Center ID table below),
    and  table-id = 128


Table file syntax
    Header = first line :
        -1:CENTER:SUBCENTER:PARAMETER_TABLE
        example (SUBCENTER undefined):
        -1:7:-1:1
        
    Body = remaining lines :
        NUMBER:NAME:DESCRIPTION [UNIT]
        example:
        2:PRMSL:Pressure reduced to MSL [Pa]


Table files content:
    File                    Center      Table Id
    ---------------         -------     ----------
    ecmwf_128.tab           ECMWF       128
    ecmwf_160.tab           ECMWF       160
    nc72_ecmwf_3.tab        ECMWF       3  (=ncep 2)
    ncep_oper_140.tab       NCEP        140
    ncep_reanal1.tab        NCEP        1  (=ncep 2)
    ncep_reanal3.tab        NCEP        3  (=ncep 2)
    ncep_reanal_2.tab       NCEP        2



Links for more info on Grib tables:
-----------------------------------

NCEP (National Centers for Environmental Prediction):
    http://wwwt.ncep.noaa.gov/

NCEP-2 :[GRIB table for the NCEP operational files as of 10-21-96]
        TABLE 2. PARAMETERS & UNITS1 & 2
        Version 2 (PDS Octet 9)
    http://wesley.wwb.noaa.gov/opn_gribtable.html


ECMWF (European Centre for Medium-Range Weather Forecasts):
    http://www.ecmwf.int/
ECMWF Grib tables index:

    http://www.ecmwf.int/publications/manuals/libraries/tables/tables_
    index.html
    128
    129 (Gradients)
    130 (ASTEX)
    131 (Probability forecast)
    140 (Wave)
    150 (Ocean - preliminary version)
    151 (Ocean - operational version)
    160 (ECMWF re-analysis statistics)
    170 (Seasonal forecasting)
    180 (ECSN - HIRETYCS)
    190 (DEMETER - provisional)




Center ID table:
----------------
3/10/98 GRIB Edition 1 (FM92) Section 1 Page 4
TABLES FOR THE PDS TABLE 0
 NATIONAL/INTERNATIONAL
 ORIGINATING CENTERS
 (Assigned By The WMO)
 (PDS Octet 5)


 VALUE CENTER
    01 Melbourne (WMC)
    02 Melbourne (WMC)
    04 Moscow (WMC)
    05 Moscow (WMC)
    07 US National Weather Service - NCEP (WMC)
    08 US National Weather Service - NWSTG (WMC)
    09 US National Weather Service - Other (WMC)
    10 Cairo (RSMC/RAFC)
    12 Dakar (RSMC/RAFC)
    14 Nairobi (RSMC/RAFC)
    16 Atananarivo (RSMC)
    18 Tunis-Casablanca (RSMC)
    20 Las Palmas (RAFC)
    21 Algiers (RSMC)
    22 Lagos (RSMC)
    26 Khabarovsk (RSMC)
    28 New Delhi (RSMC/RAFC)
    30 Novosibirsk (RSMC)
    32 Tashkent (RSMC)
    33 Jeddah (RSMC)
    34 Japanese Meteorological Agency - Tokyo (RSMC)
    36 Bankok
    37 Ulan Bator
    38 Beijing (RSMC)
    40 Seoul
    41 Buenos Aires (RSMC/RAFC)
    43 Brasilia (RSMC/RAFC)
    45 Santiago
    46 Brasilian Space Agency - INPE
    51 Miami (RSMC/RAFC)
    52 National Hurricane Center, Miami
    53 Canadian Meteorological Service - Montreal (RSMC)
    55 San Francisco
    57 U.S. Air Force - Global Weather Center
    58 US Navy - Fleet Numerical Oceanography Center
    59 NOAA Forecast Systems Lab, Boulder CO
    60 National Center for Atmospheric Research (NCAR),
       Boulder, CO
    64 Honolulu
    65 Darwin (RSMC)
    67 Melbourne (RSMC)
    69 Wellington (RSMC/RAFC)
    74 U.K. Met Office - Bracknell
    76 Moscow (RSMC/RAFC)
    78 Offenbach (RSMC)
    80 Rome (RSMC)
    82 Norrkoping
    85 French Weather Service - Toulouse
    86 Helsinki
    87 Belgrade
    88 Oslo
    89 Prague
    90 Episkopi
    91 Ankara
    92 Frankfurt/Main (RAFC)
    93 London (WAFC)
    94 Copenhagen
    95 Rota
    96 Athens
    97 European Space Agency (ESA)
    98 European Center for Medium-Range Weather
       Forecasts - Reading
    99 DeBilt, Netherlands


Note: WMC - World Meteorological Center
RSMC - Regional Specialized Meteorological Center
WAFC - World Area Forecast Center
RAFC - Regional Area Forecast Center
