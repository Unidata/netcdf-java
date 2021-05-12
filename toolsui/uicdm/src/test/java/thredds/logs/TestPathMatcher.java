package thredds.logs;

import org.junit.Test;

public class TestPathMatcher {

  @Test
  public void testStuff() {
    PathMatcher m = new PathMatcher();
    m.put("/thredds/dods/test/longer");
    m.put("/thredds/dods/test");
    m.put("/thredds/dods/tester");
    m.put("/thredds/dods/short");
    m.put("/actionable");
    m.put("myworld");
    m.put("mynot");
    m.put("ncmodels");
    m.put("ncmodels/bzipped");


    doit(m, "nope");
    doit(m, "/thredds/dods/test");
    doit(m, "/thredds/dods/test/lo");
    doit(m, "/thredds/dods/test/longer/donger");
    doit(m, "myworldly");
    doit(m, "/my");
    doit(m, "mysnot");

    doit(m, "ncmodels/canonical");
  }

  // testing
  private void doit(PathMatcher m, String s) {
    System.out.println(s + " == " + m.match(s));
  }

}
