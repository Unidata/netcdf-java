# cdm-mcidas

Contains IOSPs for gempak and mcidas iosps

## Inclusion of third-party software

This project contains source code from edu.wisc.ssec.mcidas package, and was obtained from the [VISAD GitHub repository](https://github.com/visad/visad).
The license for the edu.wisc.ssec.mcidas package is available in `docs/src/private/licenses/third-party/mcidas/`.

## Why?

The visad library does not seem to be versioned, and the associated jars are not generally available for build system consumption.
By pulling in these classes, we're able to make the gempak and mcidas iosps more generally available.

## Details of use:

To prevent package name collisions, the `edu.wisc.ssec.mcidas` was renamed to `ucar.mcidas`.
The following lines are commented out, to prevent the need to have all of visad.jar available:

AREAnav.java:

~~~
            //case LALO:
            //    anav = new LALOnav(navBlock, auxBlock);
            //    break;
~~~

CalibratorGvarG12.java

~~~
//import edu.wisc.ssec.mcidas.AncillaryData;
~~~