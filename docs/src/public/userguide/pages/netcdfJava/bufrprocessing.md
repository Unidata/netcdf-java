---
title: The Common Data Model
last_updated: 2019-06-27
sidebar: netcdfJavaTutorial_sidebar 
permalink: bufr_processing.html
toc: false
---
## CDM BUFR Processing

How to write the index.

1. Load the BUFR file into the IOSP/BUFR/BUFR tab:
{% include image.html file="netcdf-java/reference/Bufr/bufr1.png" alt="Tools UI Bufr" caption="" %} Accept the default name of <filename>.ncx.

2. Press the "Write Index" button on the upper right {% include inline_image.html file="netcdf-java/reference/Bufr/bufr2.png" alt="Save button" %}

3. Load the index file into the IOSP/BUFR/BufrCdmIndex tab. You can make changes to the ActionS column:
{% include image.html file="netcdf-java/reference/Bufr/bufr3.png" alt="Tools UI Bufr" caption="" %}

4. Press the "Write Index" button on the upper right to save your changes.

5. Now the index file will be used if you open the original file in the FeatureType/PointFeature tab.
{% include image.html file="netcdf-java/reference/Bufr/bufr4.png" alt="Tools UI Bufr" caption="" %}

We will eventually write these files automatically.