/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import java.nio.charset.StandardCharsets;
import org.bounce.text.LineNumberMargin;
import org.bounce.text.ScrollableEditorPanel;
import org.bounce.text.xml.XMLEditorKit;
import org.bounce.text.xml.XMLStyleConstants;
import org.jdom2.Element;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.dialog.NetcdfOutputChooser;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.IO;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.ComboBox;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.text.PlainDocument;

/**
 * An Editor for NcML files.
 *
 * @author caron
 * @since 3/13/13
 */
public class NcmlEditor extends JPanel {
  private static final boolean debugNcmlWrite = false;

  private NetcdfDataset ds;
  private String ncmlLocation;
  private JEditorPane editor;
  private Map<String, String> protoMap = new HashMap<>(10);
  private ComboBox<String> protoChooser;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private FileManager fileChooser;
  private NetcdfOutputChooser outChooser;

  private final AbstractButton coordButt;
  private boolean addCoords;

  private PreferencesExt prefs;


  public NcmlEditor(JPanel buttPanel, PreferencesExt prefs) {
    this.prefs = prefs;
    fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

    AbstractAction coordAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        addCoords = (Boolean) getValue(BAMutil.STATE);
        String tooltip = addCoords ? "add Coordinates is ON" : "add Coordinates is OFF";
        coordButt.setToolTipText(tooltip);
      }
    };
    addCoords = prefs.getBoolean("coordState", false);
    String tooltip2 = addCoords ? "add Coordinates is ON" : "add Coordinates is OFF";
    BAMutil.setActionProperties(coordAction, "nj22/AddCoords", tooltip2, true, 'C', -1);
    coordAction.putValue(BAMutil.STATE, addCoords);
    coordButt = BAMutil.addActionToContainer(buttPanel, coordAction);

    protoChooser = new ComboBox<>((PreferencesExt) prefs.node("protoChooser"));
    addProtoChoices();
    buttPanel.add(protoChooser);
    protoChooser.addActionListener(e -> {
      String ptype = (String) protoChooser.getSelectedItem();
      String proto = protoMap.get(ptype);
      if (proto != null) {
        editor.setText(proto);
      }
    });

    editor = new JEditorPane();

    // Instantiate a XMLEditorKit with wrapping enabled.
    XMLEditorKit kit = new XMLEditorKit();

    // Enable auto indentation.
    kit.setAutoIndentation(true);

    // Enable tag completion.
    kit.setTagCompletion(true);

    // Set a style
    kit.setStyle(XMLStyleConstants.ATTRIBUTE_NAME, Color.RED, Font.BOLD);

    editor.setEditorKit(kit);

    // Set the font style.
    editor.setFont(new Font("Monospaced", Font.PLAIN, 12));

    // Set the tab size
    editor.getDocument().putProperty(PlainDocument.tabSizeAttribute, 2);

    // Put the editor in a panel that will force it to resize, when a different view is choosen.
    ScrollableEditorPanel editorPanel = new ScrollableEditorPanel(editor);

    JScrollPane scroller = new JScrollPane(editorPanel);

    // Add the number margin as a Row Header View
    scroller.setRowHeaderView(new LineNumberMargin(editor));

    AbstractAction saveAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String location = (ds == null) ? ncmlLocation : ds.getLocation();
        if (location == null) {
          location = "test";
        }

        int pos = location.lastIndexOf(".");
        if (pos > 0) {
          location = location.substring(0, pos);
        }

        String filename = fileChooser.chooseFilenameToSave(location + ".ncml");
        if (filename == null) {
          return;
        }
        if (doSaveNcml(editor.getText(), filename)) {
          ncmlLocation = filename;
        }
      }
    };
    BAMutil.setActionProperties(saveAction, "Save", "Save NcML", false, 'S', -1);
    BAMutil.addActionToContainer(buttPanel, saveAction);

    AbstractAction netcdfAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (outChooser == null) {
          outChooser = new NetcdfOutputChooser((Frame) null);
          outChooser.addPropertyChangeListener("OK", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
              writeNetcdf((NetcdfOutputChooser.Data) evt.getNewValue());
            }
          });
        }

        String location = (ds == null) ? ncmlLocation : ds.getLocation();
        if (location == null) {
          location = "test";
        }

        int pos = location.lastIndexOf(".");
        if (pos > 0) {
          location = location.substring(0, pos);
        }
        outChooser.setOutputFilename(location);
        outChooser.setVisible(true);
      }
    };
    BAMutil.setActionProperties(netcdfAction, "nj22/Netcdf", "Write netCDF file", false, 'N', -1);
    BAMutil.addActionToContainer(buttPanel, netcdfAction);

    AbstractAction transAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doTransform(editor.getText());
      }
    };
    BAMutil.setActionProperties(transAction, "Import",
        "read textArea through NcMLReader\n write NcML back out via resulting dataset", false, 'T', -1);
    BAMutil.addActionToContainer(buttPanel, transAction);

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Check NcML", false);
    compareButton.addActionListener(e -> {
      Formatter f = new Formatter();
      checkNcml(f);

      infoTA.setText(f.toString());
      infoTA.gotoTop();
      infoWindow.show();
    });
    buttPanel.add(compareButton);

    setLayout(new BorderLayout());
    add(scroller, BorderLayout.CENTER);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));
  }

  public void save() {
    fileChooser.save();
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
  }

  public void closeOpenFiles() {
    try {
      if (ds != null) {
        ds.close();
      }
    } catch (IOException ioe) {
      // Nothing to do here.
    }
    ds = null;
  }

  public boolean setNcml(String cmd) {
    if (cmd.endsWith(".xml") || cmd.endsWith(".ncml")) {
      if (!cmd.startsWith("http:") && !cmd.startsWith("file:")) {
        cmd = "file:" + cmd;
      }
      ncmlLocation = cmd;
      String text = IO.readURLcontents(cmd);
      editor.setText(text);
    } else {
      writeNcml(cmd);
    }
    return true;
  }

  /**
   * write ncml from given dataset
   */
  boolean writeNcml(String location) {
    boolean err = false;

    closeOpenFiles();

    try {
      String result;
      ds = openDataset(location, addCoords, null);
      if (ds == null) {
        editor.setText("Failed to open <" + location + ">");
      } else {
        NcMLWriter ncmlWriter = new NcMLWriter();
        Element netcdfElem = ncmlWriter.makeNetcdfElement(ds, null);
        result = ncmlWriter.writeToString(netcdfElem);

        editor.setText(result);
        editor.setCaretPosition(0);
      }
    } catch (Exception e) {
      StringWriter sw = new StringWriter(10000);
      e.printStackTrace();
      e.printStackTrace(new PrintWriter(sw));
      editor.setText(sw.toString());
      err = true;
    }

    return !err;
  }

  private NetcdfDataset openDataset(String location, boolean addCoords, CancelTask task) {
    try {
      boolean useBuilders = ToolsUI.getToolsUI().getUseBuilders();
      return useBuilders ? NetcdfDatasets.openDataset(location, addCoords, task)
          : NetcdfDataset.openDataset(location, addCoords, task);

      // if (setUseRecordStructure)
      // ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "NetcdfDataset.open cannot open " + ioe.getMessage());
      if (!(ioe instanceof FileNotFoundException)) {
        ioe.printStackTrace();
      }
      return null;
    }
  }

  void writeNetcdf(NetcdfOutputChooser.Data data) {
    String text = editor.getText();

    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
      NcMLReader.writeNcMLToFile(bis, data.outputFilename, data.version,
          Nc4ChunkingStrategy.factory(data.chunkerType, data.deflate, data.shuffle));
      JOptionPane.showMessageDialog(this, "File successfully written");

    } catch (Exception exc) {
      JOptionPane.showMessageDialog(this, "ERROR: " + exc.getMessage());
      exc.printStackTrace();
    }
  }

  /**
   * Read text from textArea through NcMLReader
   * then write it back out via resulting dataset
   */
  void doTransform(String text) {
    try (StringReader reader = new StringReader(text);
        NetcdfDataset ncd = NcMLReader.readNcML(reader, null);
        StringWriter sw = new StringWriter(10000)) {
      ncd.writeNcML(sw, null);
      editor.setText(sw.toString());
      editor.setCaretPosition(0);
      JOptionPane.showMessageDialog(this, "File successfully transformed");
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  /**
   * Read text from textArea through NcMLReader
   * then write it back out via resulting dataset
   */
  private void checkNcml(Formatter f) {
    if (ncmlLocation == null) {
      return;
    }
    boolean useBuilders = ToolsUI.getToolsUI().getUseBuilders();
    try (NetcdfDataset ncd =
        useBuilders ? NetcdfDatasets.openDataset(ncmlLocation) : NetcdfDataset.openDataset(ncmlLocation)) {
      ncd.check(f);
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  boolean doSaveNcml(String text, String filename) {
    if (debugNcmlWrite) {
      System.out.println("filename=" + filename);
      System.out.println("text=" + text);
    }

    File out = new File(filename);
    if (out.exists()) {
      int val = JOptionPane.showConfirmDialog(null, filename + " already exists. Do you want to overwrite?", "WARNING",
          JOptionPane.YES_NO_OPTION);
      if (val != JOptionPane.YES_OPTION) {
        return false;
      }
    }

    try {
      IO.writeToFile(text, out);
      JOptionPane.showMessageDialog(this, "File successfully written");
      return true;
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
      return false;
    }
    // saveNcmlDialog.setVisible(false);
  }

  void addProtoChoices() {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n"
        + "  <variable name='time' type='int' shape='time'>\n"
        + "    <attribute name='long_name' type='string' value='time coordinate' />\n"
        + "    <attribute name='units' type='string' value='days since 2001-8-31 00:00:00 UTC' />\n"
        + "    <values start='0' increment='10' />\n" + "  </variable>\n"
        + "  <aggregation dimName='time' type='joinNew'>\n" + "    <variableAgg name='T'/>\n"
        + "    <scan location='src/test/data/ncml/nc/' suffix='.nc' subdirs='false'/>\n" + "  </aggregation>\n"
        + "</netcdf>";
    protoMap.put("joinNew", xml);
    protoChooser.addItem("joinNew");

    xml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n"
        + "  <aggregation dimName='time' type='joinExisting'>\n"
        + "    <scan location='ncml/nc/pfeg/' suffix='.nc' />\n" + "  </aggregation>\n" + "</netcdf>";
    protoMap.put("joinExisting", xml);
    protoChooser.addItem("joinExisting");
  }
}
