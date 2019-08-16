package ucar.unidata.util;

import org.junit.Test;

public class TestGaussianLatitudes {


  // should match http://dss.ucar.edu/datasets/common/ecmwf/ERA40/docs/std-transformations/dss_code_glwp.html
  @Test
  public void testStuff() {
    int nlats = 94;
    GaussianLatitudes glat = GaussianLatitudes.factory(nlats);
    for (int i=0; i<nlats; i++) {
      System.out.print(" lat "+i+" = "+ Format.dfrac( glat.latd[i], 4));
      if (i < nlats - 1)
        System.out.print(" diff = " + (glat.latd[i + 1] - glat.latd[i]));
      System.out.println(" weight= "+ Format.dfrac( glat.gaussw[i], 6));
    }
  }

}
