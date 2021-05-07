/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import org.slf4j.Logger;
import thredds.filesystem.MFileOS7;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * A MCollection defined by a glob filter. Experimental.
 *
 * From http://blog.eyallupu.com/2011/11/java-7-working-with-directories.html
 *
 * The 'glob' Syntax
 * The glob (stands for globbing) syntax is a 'simplified' form of regular expressions with awareness to path
 * components (directories), the syntax is composed of the following syntactic tokens:
 * 1) The '*' character matches zero or more characters from the path elements without crossing directory boundaries
 * (unlike regular expression this is not a Kleene star and it has nothing to do with the preceding part of the
 * expression)
 * 2) The '**' characters match zero or more characters crossing directory boundaries
 * 3) The '?' character matches exactly one character of a name component
 * 4) '[' and ']' can be used to match a single character in the path name from a set of characters
 * '-' (hyphen) can be used to specify a range of characters. If hyphen has to be included in the characters set it must
 * be the first in the set
 * '!' as the first character in the set can be used as a negation expression
 * 5) '{' and '}' can group sub patterns, the group matches if any of the sub patterns matches (comma is used to
 * separate between the groups)
 * 6 ) The dot '.' character represents a dot (unlike regular expressions in which a dot is a replacement for any
 * character)
 * 7) and finally: special characters escaping is done using backslash
 * 
 * The '**' expression is the only one to cross directory boundaries, all other expressions are bound within a single
 * element
 * (either a directory or a filename), below is a sample usage of PathMatcher followed by few pattern examples:
 *
 *
 *
 * 
 * @author caron
 * @since 5/19/14
 */
public class CollectionGlob extends CollectionAbstract {
  PathMatcher matcher;
  boolean debug;
  int depth;

  public CollectionGlob(String collectionName, String glob, Logger logger) {
    super(collectionName, logger);

    matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);

    // lets suppose the first "*" indicates the top dir
    int pos = glob.indexOf("*");
    this.root = glob.substring(0, pos - 1);
    String match = glob.substring(pos);

    // count how far to recurse. LAME!!! why doesnt java provide the right thing !!!!
    pos = glob.indexOf("**");
    if (pos > 0)
      depth = Integer.MAX_VALUE;
    else {
      // count the "/" !!
      for (char c : match.toCharArray())
        if (c == '/')
          depth++;
    }

    if (debug)
      System.out.printf(" CollectionGlob.MFileIterator topPath='%s' depth=%d%n", this.root, this.depth);

  }

  @Override
  public void close() {

  }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return makeFileListSorted();
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MyFileIterator(this.root);
  }

  // from http://blog.eyallupu.com/2011/11/java-7-working-with-directories.html
  public static DirectoryStream<Path> newDirectoryStream(Path dir, String glob) throws IOException {
    FileSystem fs = dir.getFileSystem();
    PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
    DirectoryStream.Filter<Path> filter = entry -> matcher.matches(entry.getFileName());
    return fs.provider().newDirectoryStream(dir, filter);
  }

  private class MyFileIterator implements CloseableIterator<MFile> {
    DirectoryStream<Path> dirStream;
    Iterator<Path> dirStreamIterator;
    MFile nextMFile;
    int count, total;
    Stack<Path> subdirs = new Stack<>();
    int currDepth;

    MyFileIterator(String topDir) throws IOException {
      Path topPath = Paths.get(topDir);
      dirStream = Files.newDirectoryStream(topPath);
      dirStreamIterator = dirStream.iterator();
    }

    public boolean hasNext() {
      while (true) {

        try {
          while (!dirStreamIterator.hasNext()) {
            dirStream.close();
            if (subdirs.isEmpty()) {
              nextMFile = null;
              return false;
            }
            currDepth++; // LOOK wrong
            Path nextSubdir = subdirs.pop();
            dirStream = Files.newDirectoryStream(nextSubdir);
            dirStreamIterator = dirStream.iterator();
          }

          total++;
          Path nextPath = dirStreamIterator.next();
          BasicFileAttributes attr = Files.readAttributes(nextPath, BasicFileAttributes.class);
          if (attr.isDirectory()) {
            if (currDepth < depth)
              subdirs.push(nextPath);
            continue;
          }

          if (!matcher.matches(nextPath)) {
            continue;
          }

          nextMFile = new MFileOS7(nextPath, attr);
          return true;

        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        // if (filter == null || filter.accept(nextMFile)) return true;
      }
    }

    public MFile next() {
      if (nextMFile == null)
        throw new NoSuchElementException();
      count++;
      return nextMFile;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    // better alternative is for caller to send in callback (Visitor pattern)
    // then we could use the try-with-resource
    public void close() {
      if (debug)
        System.out.printf("  OK=%d total=%d%n ", count, total);
      try {
        dirStream.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
