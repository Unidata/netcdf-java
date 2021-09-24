/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import ucar.ma2.Array;
import ucar.ma2.IsMissingEvaluator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Variable;
import ucar.nc2.write.Ncdump;
import ucar.ui.widget.*;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.ComboBox;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Dump data using NetcdfFile.readSection() */
public class NCdumpPane extends TextHistoryPane {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ComboBox<String> cb;
  private CommonTask task;
  private final StopButton stopButton;
  private final FileManager fileChooser;

  private NetcdfFile ds;

  public NCdumpPane(PreferencesExt prefs) {
    super(true);
    fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

    cb = new ComboBox<>(prefs);

    JButton getButton = new JButton("NCdump");
    getButton.setToolTipText("show selected data values");
    getButton.addActionListener(e -> ncdump((String) cb.getSelectedItem()));

    JButton imageButton = new JButton("Image");
    imageButton.setToolTipText("view selected data as Image");
    imageButton.addActionListener(e -> showImage((String) cb.getSelectedItem()));

    stopButton = new StopButton("stop NCdump");
    stopButton.addActionListener(e -> {
      ta.setText(task.v.toString());
      ta.append("\n data:\n");
      ta.append(task.contents);

      if (e.getActionCommand().equals("success")) {
        cb.setSelectedItem(task.command); // add to combobox
      }
    });

    JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    buttPanel.add(getButton);
    buttPanel.add(imageButton);
    buttPanel.add(stopButton);

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(new JLabel("Variable:"), BorderLayout.WEST);
    topPanel.add(cb, BorderLayout.CENTER);
    topPanel.add(buttPanel, BorderLayout.EAST);

    add(topPanel, BorderLayout.NORTH);
  }

  public void setContext(NetcdfFile ds, String command) {
    this.ds = ds;
    cb.addItem(command);
  }

  private void ncdump(String command) {
    if (ds == null) {
      return;
    }
    if (command == null) {
      return;
    }

    task = new NCdumpTask(command);
    if (task.v != null) {
      stopButton.startProgressMonitorTask(task);
    }
  }

  private void showImage(String command) {
    if (ds == null) {
      return;
    }
    if (command == null) {
      return;
    }

    task = new GetContentsTask(command);
    if (task.v != null) {
      stopButton.startProgressMonitorTask(task);
    }
  }

  public void save() {
    cb.save();
    fileChooser.save();
  }

  public void clear() {
    ta.setText(null);
  }

  public String getText() {
    return ta.getText();
  }

  public void gotoTop() {
    ta.setCaretPosition(0);
  }

  public void setText(String text) {
    ta.setText(text);
  }

  private abstract class CommonTask extends ProgressMonitorTask implements ucar.nc2.util.CancelTask {
    String contents, command;
    Variable v;
    Array data;
    IsMissingEvaluator eval;

    CommonTask(String command) {
      this.command = command;
      try {
        ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ds, command);
        while (cer != null) { // get inner variable
          v = cer.getVariable();
          cer = cer.getChild();
        }
        if (v instanceof IsMissingEvaluator) {
          eval = (IsMissingEvaluator) v;
        }
      } catch (Exception e) {
        ta.setText(e.getMessage());
      }
    }
  }

  private class GetContentsTask extends CommonTask {

    GetContentsTask(String command) {
      super(command);
    }

    public void run() {
      StringWriter sw = new StringWriter(100000);
      PrintWriter ps = new PrintWriter(sw);
      try {
        data = ds.readSection(command);
      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintWriter(sw));
        contents = sw.toString();

        setError(e.getMessage());
        done = true;
        return;
      }

      if (cancel) {
        ps.println("\n***Cancelled by User");
      }
      contents = sw.toString();

      success = !cancel;
      done = true;
    }
  }

  private class NCdumpTask extends CommonTask {

    NCdumpTask(String command) {
      super(command);
    }

    public void run() {
      try {
        data = ds.readSection(command);
        contents = Ncdump.printArray(data, null, this);
      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(100000);
        e.printStackTrace(new PrintWriter(sw));
        contents = sw.toString();

        setError(e.getMessage());
        done = true;
        return;
      }

      if (cancel) {
        contents = "\n***Cancelled by User";
      }

      success = !cancel;
      done = true;
    }
  }
}
