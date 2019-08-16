/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import ucar.util.prefs.PreferencesExt;

/**
 * Cover for JFileChooser.
 * <p/>
 * <pre>
 * <p/>
   javax.swing.filechooser.FileFilter[] filters = new javax.swing.filechooser.FileFilter[2];
   filters[0] = new FileManager.HDF5ExtFilter();
   filters[1] = new FileManager.NetcdfExtFilter();
   fileChooser = new FileManager(parentFrame, null, filters, (PreferencesExt) prefs.node("FileManager"));

   AbstractAction fileAction =  new AbstractAction() {
     public void actionPerformed(ActionEvent e) {
       String filename = fileChooser.chooseFilename();
       if (filename == null) return;
       process(filename);
     }
   };
   BAMutil.setActionProperties( fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
   </pre>
 *
 * @author John Caron
 */

public class FileManager {
  private static final String BOUNDS = "Bounds";
  private static final String DEFAULT_DIR = "DefaultDir";
  private static final String DEFAULT_FILTER = "DefaultFilter";

  // regular
  private PreferencesExt prefs;
  private IndependentDialog w;
  private ucar.ui.prefs.ComboBox<String> dirComboBox;
  private JFileChooser chooser = null;

  // for override
  protected JPanel main;
  //protected boolean selectedURL = false;
  //protected ComboBox urlComboBox;

  private boolean readOk = true, selectedFile = false;
  private static boolean debug = false;

  public FileManager(JFrame parent) {
    this(parent, null, null, null);
  }

  public FileManager(JFrame parent, String defDir) {
    this(parent, defDir, null, null);
  }

  public FileManager(JFrame parent, String defDir, String file_extension, String desc, PreferencesExt prefs) {
    this(parent, defDir, new FileFilter[]{new ExtFilter(file_extension, desc)}, prefs);
  }

  public FileManager(JFrame parent, String defDir, FileFilter[] filters, PreferencesExt prefs) {
    this.prefs = prefs;

    // where to start ?
    java.util.List<String> defaultDirs = new ArrayList<>();
    if (defDir != null)
      defaultDirs.add(defDir);
    else {
      String dirName = (prefs != null) ? prefs.get(DEFAULT_DIR, ".") : ".";
      defaultDirs.add(dirName);
    }

    /* funky windows workaround
    String osName = System.getProperty("os.name");
    //System.out.println("OS ==  "+ osName+" def ="+defDir);
    boolean isWindose = (0 <= osName.indexOf("Windows"));
    if (isWindose)
      defaultDirs.add("C:/"); */

    File defaultDirectory = findDefaultDirectory(defaultDirs);
    try {
      chooser = new ImprovedFileChooser(defaultDirectory);

    } catch (SecurityException se) {
      System.out.println("FileManager SecurityException " + se);
      readOk = false;
      JOptionPane.showMessageDialog(null, "Sorry, this Applet does not have disk read permission.");
    }

    chooser.addActionListener(e -> {
        if (debug) System.out.println("**** chooser event=" + e.getActionCommand() + "\n  " + e);
        //if (debug) System.out.println("  curr directory="+chooser.getCurrentDirectory());
        //if (debug) System.out.println("  selected file="+chooser.getSelectedFile());

        if (e.getActionCommand().equals("ApproveSelection"))
          selectedFile = true;
        w.setVisible(false);
    });

    // set filters
    if (filters != null) {
      for (FileFilter filter : filters) {
        chooser.addChoosableFileFilter(filter);
      }
    }

    // saved file filter
    if (prefs != null) {
      String wantFilter = prefs.get(DEFAULT_FILTER, null);
      if (wantFilter != null) {
        for (FileFilter fileFilter : chooser.getChoosableFileFilters()) {
          if (fileFilter.getDescription().equals(wantFilter))
            chooser.setFileFilter(fileFilter);
        }
      }
    }

    // buttcons
    AbstractAction usedirAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        String item = (String) dirComboBox.getSelectedItem();
        // System.out.println(" cb =  "+item);
        if (item != null)
          chooser.setCurrentDirectory(new File(item));
      }
    };
    BAMutil.setActionProperties(usedirAction, "FingerDown", "use this directory", false, 'U', -1);

    AbstractAction savedirAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        File currDir = chooser.getCurrentDirectory();
        //System.out.println("  curr directory="+currDir);
        if (currDir != null)
          dirComboBox.addItem(currDir.getPath());
      }
    };
    BAMutil.setActionProperties(savedirAction, "FingerUp", "save current directory", false, 'S', -1);

    AbstractAction rescanAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        chooser.rescanCurrentDirectory();
      }
    };
    BAMutil.setActionProperties(rescanAction, "Undo", "refresh", false, 'R', -1);

    // put together the UI
    JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    BAMutil.addActionToContainer(buttPanel, usedirAction);
    BAMutil.addActionToContainer(buttPanel, savedirAction);
    BAMutil.addActionToContainer(buttPanel, rescanAction);

    JPanel dirPanel = new JPanel(new BorderLayout());
    dirComboBox = new ucar.ui.prefs.ComboBox<>(prefs);
    dirComboBox.setEditable(true);
    dirPanel.add(new JLabel(" Directories: "), BorderLayout.WEST);
    dirPanel.add(dirComboBox, BorderLayout.CENTER);
    dirPanel.add(buttPanel, BorderLayout.EAST);

    main = new JPanel(new BorderLayout());
    main.add(dirPanel, BorderLayout.NORTH);
    main.add(chooser, BorderLayout.CENTER);

    /* urlComboBox = new ComboBox(prefs);
    urlComboBox.addActionListener(e -> {
          selectedURL = true;
    });

    JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    p.add( new JLabel("or a URL:"));
    p.add( urlComboBox);
    main.add(urlComboBox, BorderLayout.SOUTH); */

    //w = new IndependentWindow("FileChooser", BAMutil.getImage("FileChooser"), main);
    w = new IndependentDialog(parent, true, "FileChooser", main);
    if (null != prefs) {
      Rectangle b = (Rectangle) prefs.getObject(BOUNDS);
      if (b != null)
        w.setBounds(b);
    }
  }

  public void save() {
    if (prefs == null) return;

    File currDir = chooser.getCurrentDirectory();
    if (currDir != null)
      prefs.put(DEFAULT_DIR, currDir.getPath());

    FileFilter currFilter = chooser.getFileFilter();
    if (currFilter != null)
      prefs.put(DEFAULT_FILTER, currFilter.getDescription());

    if (dirComboBox != null)
      dirComboBox.save();

    prefs.putObject(BOUNDS, w.getBounds());
  }

  public JFileChooser getFileChooser() {
    return chooser;
  }

  /* public java.io.File chooseFile() {
   if (!readOk) return null;
   w.show();

   if (chooser.showOpenDialog( parent) == JFileChooser.APPROVE_OPTION) {
     File file = chooser.getSelectedFile();
     if (debug) System.out.println("FileManager result "+file.getPath());
     if (file != null)
       return file;
   }
   return null;
 } */

  public String chooseFilenameToSave(String defaultFilename) {
    chooser.setDialogType(JFileChooser.SAVE_DIALOG);
    String result = (defaultFilename == null) ? chooseFilename() : chooseFilename(defaultFilename);
    chooser.setDialogType(JFileChooser.OPEN_DIALOG);

    return result;
  }

  public String chooseDirectory(String defaultDirectory) {
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    String result = (defaultDirectory == null) ? chooseFilename() : chooseFilename(defaultDirectory);
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    return result;
  }

  /**
   * Allow user to select file, then return the filename, in canonical form,
   * always using '/', never '\'
   *
   * @return chosen filename in canonical form, or null if nothing chosen.
   */
  @Nullable
  public String chooseFilename() {
    if (!readOk) return null;
    selectedFile = false;
    //selectedURL = false;
    w.setVisible(true); // modal, so blocks; listener calls hide(), which unblocks.

    if (selectedFile) {
      File file = chooser.getSelectedFile();
      if (file == null) return null;
      try {
        return file.getCanonicalPath().replace('\\', '/');
      } catch (IOException ioe) {
      } // return null
    }

    /* if (selectedURL) {
      return (String) urlComboBox.getSelectedItem();
    }  */

    return null;
  }

  public String chooseFilename(File def) {
    File parent = def.getParentFile();
    chooser.setCurrentDirectory(parent);
    chooser.setSelectedFile(def);
    return chooseFilename();
  }

  public String chooseFilename(String defaultFilename) {
    chooser.setSelectedFile(new File(defaultFilename));
    return chooseFilename();
  }

  public File[] chooseFiles() {
    chooser.setMultiSelectionEnabled(true);
    selectedFile = false;
    w.setVisible(true);
    
    if (selectedFile)
      return chooser.getSelectedFiles();

    return null;
  }

  public String getCurrentDirectory() {
    return chooser.getCurrentDirectory().getPath();
  }

  public void setCurrentDirectory(String dirName) {
    File dir = new File(dirName);
    chooser.setCurrentDirectory(dir);
  }

  private File findDefaultDirectory(java.util.List<String> tryDefaultDirectories) {
    boolean readOK = true;
    for (String tryDefaultDirectory : tryDefaultDirectories) {
      try {
        if (debug) System.out.print("FileManager try " + tryDefaultDirectory);
        File dir = new File(tryDefaultDirectory);
        if (dir.exists()) {
          if (debug) System.out.println(" = ok ");
          return dir;
        } else {
          if (debug) System.out.println(" = no ");
        }
      } catch (SecurityException se) {
        if (debug) System.out.println("SecurityException in FileManager: " + se);
        readOK = false;
      }
    }

    if (!readOK)
      JOptionPane.showMessageDialog(null, "Sorry, this Applet does not have disk read permission.");
    return null;
  }

  public static class ExtFilter extends FileFilter {
    String file_extension;
    String desc;

    public ExtFilter(String file_extension, String desc) {
      this.file_extension = file_extension;
      this.desc = desc;
    }

    public boolean accept(File file) {
      if (null == file_extension)
        return true;
      String name = file.getName();
      return file.isDirectory() || name.endsWith(file_extension);
    }

    public String getDescription() {
      return desc;
    }
  }

  public static class NetcdfExtFilter extends FileFilter {

    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      return file.isDirectory() || name.endsWith(".nc") || name.endsWith(".cdf");
    }

    public String getDescription() {
      return "netcdf";
    }
  }

  public static class HDF5ExtFilter extends FileFilter {

    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      return file.isDirectory() || name.endsWith(".h5") || name.endsWith(".hdf");
    }

    public String getDescription() {
      return "hdf5";
    }
  }

  public static class XMLExtFilter extends FileFilter {

    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      return file.isDirectory() || name.endsWith(".xml");
    }

    public String getDescription() {
      return "xml";
    }
  }
}