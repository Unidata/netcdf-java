package thredds.ui.widget;

import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.JFrame;
import org.junit.Test;
import ucar.nc2.ui.widget.URLDumpPane;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

public class TestUrlDumpPanel {

  private static XMLStore xstore;
  private static URLDumpPane main;

  @Test
  public void testStuff() {
    try {
      JFrame frame = new JFrame("URL Dump Pane");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          try {
            main.save();
            xstore.save();
          } catch (IOException ioe) {
            ioe.printStackTrace();
          }
          System.exit(0);
        }
      });

      // java.net.Authenticator.setDefault(new UrlAuthenticatorDialog(frame));

      // open up the preferences file(s)
      try {
        String storeFilename = XMLStore.makeStandardFilename(".unidata", "URLDumpPane.xml");
        xstore = XMLStore.createFromFile(storeFilename, null);
      } catch (java.io.IOException e) {
        e.printStackTrace();
      }
      PreferencesExt store = xstore.getPreferences();

      main = new URLDumpPane(store);

      frame.getContentPane().add(main);
      frame.pack();
      frame.setLocation(200, 200);
      frame.setSize(900, 700);
      frame.setVisible(true);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

}
