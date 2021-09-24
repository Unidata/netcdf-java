/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.image;

import java.util.*;
import java.net.*;

public class PictureCache {

  /**
   * hashtable to facilitate the caching of images. It is static
   * so that there is just one for the application
   */
  private static Hashtable pictureCache = new Hashtable();


  /**
   * This Vector keeps track of which pictures the PictureCache has been
   * requested to load in the background. They may have to be stopped in
   * a hurry.
   */
  public static Vector cacheLoadsInProgress = new Vector();


  /**
   * Vector to keep track of what we should remove first in the cache
   */
  private static Vector removalQueue = new Vector();


  private static int maxCache = 10; // was Settings


  /**
   * this method removes the least popular picture(s) in the cache.
   * It first removes those pictures which have been suggested for
   * removal. And then it picks any it can find
   * As many pictures are removed as nescessary untill there are less pictures in the
   * cache than the Settings.maxCache specifies. (If maxCache is 0 then the
   * Enumeration finds no elements and we don't get an endless loop.
   */
  public static synchronized void removeLeastPopular() {
    Tools.log("PictureCache.removeLeastPopular:");
    // reportCache();

    Enumeration e = removalQueue.elements();
    while ((e.hasMoreElements()) && (pictureCache.size() >= maxCache)) {
      String removeElement = (String) e.nextElement();
      Tools.log("PictureCache.remove: " + removeElement);
      pictureCache.remove(removeElement);
      removalQueue.remove(removeElement);
    }

    e = pictureCache.keys();
    while ((pictureCache.size() >= maxCache) && (e.hasMoreElements())) {
      String removeElement = (String) e.nextElement();
      Tools.log("PictureCache.remove: " + removeElement);
      pictureCache.remove(removeElement);
    }
  }

  /**
   * returns whether an image is in the cache.
   * <p>
   */
  public static synchronized boolean isInCache(URL url) {
    return isInCache(url.toString());
  }

  /**
   * returns whether an image is in the cache.
   * <p>
   */
  public static synchronized boolean isInCache(String urlString) {
    return pictureCache.containsKey(urlString);
  }

  /**
   * store an image in the cache
   * 
   * @param url The URL of the picture
   * @param sp The picture to be stored
   */
  public static synchronized void add(URL url, SourcePicture sp) {
    Tools.log("PictureCache.add: " + url);
    if (sp.getSourceBufferedImage() == null) {
      Tools.log("PictureCache.add: invoked with a null picture! Not cached!");
      return;
    }

    if ((maxCache < 1)) {
      Tools.log("PictureCache.add: cache is diabled. Not adding picture.");
      return;
    }

    if (isInCache(url)) {
      Tools.log("Picture " + url + " is already in the cache. Not adding again.");
      return;
    }

    if (pictureCache.size() >= maxCache)
      removeLeastPopular();

    if (pictureCache.size() < maxCache)
      pictureCache.put(url.toString(), sp);
  }



  /**
   * remove a picture from the cache
   */
  public static synchronized void remove(String urlString) {
    if (isInCache(urlString)) {
      pictureCache.remove(urlString);
    }
  }


  /**
   * returns a picture from the cache. Returns null if image is not there
   * 
   * @param url The URL of the picture to be retrieved
   */
  public static synchronized SourcePicture getSourcePicture(URL url) {
    return (SourcePicture) pictureCache.get(url.toString());
  }

  /**
   * clears out all images in the cache. Important after OutOfMemoryErrors
   */
  public static synchronized void clear() {
    Tools.log("PictureCache.clear: Zapping entire cache");
    pictureCache.clear();
  }

  /**
   * method to inspect the cache
   */
  public static synchronized void reportCache() {
    Tools.log("   PictureCache.reportCache: cache contains: " + pictureCache.size() + " max: " + maxCache);
    // Tools.freeMem();
    Enumeration e = pictureCache.keys();
    while (e.hasMoreElements()) {
      Tools.log("   Cache contains: " + e.nextElement());
    }
    Tools.log("  End of cache contents");
  }

  /**
   * method to stop all background loading
   */
  public static void stopBackgroundLoading() {
    Enumeration e = cacheLoadsInProgress.elements();
    while (e.hasMoreElements()) {
      ((SourcePicture) e.nextElement()).stopLoading();
    }
  }

  /**
   * method to stop all background loading except the indicated file. Returns whether the
   * image is already being loaded. True = loading in progress, False = not in progress.
   */
  public static boolean stopBackgroundLoadingExcept(URL exemptionURL) {
    SourcePicture sp;
    String exemptionURLString = exemptionURL.toString();
    Enumeration e = cacheLoadsInProgress.elements();
    boolean inProgress = false;
    while (e.hasMoreElements()) {
      sp = ((SourcePicture) e.nextElement());
      if (!sp.getUrlString().equals(exemptionURLString))
        sp.stopLoading();
      else {
        Tools.log("PictureCache.stopBackgroundLoading: picture was already loading");
        inProgress = true;
      }
    }
    return inProgress;
  }

}
