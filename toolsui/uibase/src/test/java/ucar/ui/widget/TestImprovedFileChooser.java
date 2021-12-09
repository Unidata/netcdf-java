package ucar.ui.widget;

import java.awt.Dimension;
import java.awt.HeadlessException;
import javax.swing.UIManager;
import org.junit.Test;

public class TestImprovedFileChooser {

  @Test
  public void testStuff() throws Exception {
    try {
      if (ImprovedFileChooser.isMacOs) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
      } else {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
          if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
          }
        }
      }
      ImprovedFileChooser fileChooser = new ImprovedFileChooser();
      fileChooser.setPreferredSize(new Dimension(1000, 750));
      fileChooser.createDialog(null);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

}
