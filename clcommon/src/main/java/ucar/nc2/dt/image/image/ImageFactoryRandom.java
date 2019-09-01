/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt.image.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read in all images in a dir and subdirs, and randomly iterate.
 *
 * @author caron
 * @since Oct 9, 2008
 */
public class ImageFactoryRandom {
  private static Logger logger = LoggerFactory.getLogger(ImageFactoryRandom.class);
  private java.util.List<File> holdList;
  private java.util.List<File> fileList;
  private Random random = new Random(System.currentTimeMillis());

  public ImageFactoryRandom(File topDir) {
    if (!topDir.exists())
      return;

    fileList = new ArrayList<>(1000);
    addToList(topDir, fileList);
    holdList = new ArrayList<>(fileList);
  }

  private void addToList(File dir, List<File> list) {
    for (File file : dir.listFiles()) {
      if (file.isDirectory())
        addToList(file, list);
      else if (file.getName().endsWith(".jpg") || file.getName().endsWith(".JPG"))
        list.add(file);
    }
  }

  File nextFile = null;

  public BufferedImage getNextImage() {
    if (holdList.isEmpty())
      holdList = new ArrayList<>(fileList);

    int next = random.nextInt(holdList.size());
    nextFile = holdList.get(next);
    holdList.remove(nextFile); // random draw without replacement

    try {
      return javax.imageio.ImageIO.read(nextFile);
    } catch (IOException e) {
      logger.warn("Failed to open image {}", nextFile);
      fileList.remove(nextFile);
      return getNextImage();
    }
  }

  // remove last file
  public boolean delete() {
    if (nextFile == null)
      return false;
    fileList.remove(nextFile);
    File f = new File("C:/tmp/deleted/" + nextFile.getName());
    return nextFile.renameTo(f);
  }


}
