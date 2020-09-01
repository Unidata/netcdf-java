/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servlet;

import java.io.Closeable;
import opendap.dap.DAS;
import opendap.dap.parsers.ParseException;
import thredds.server.opendap.servers.ServerDDS;


/**
 * A GuardedDataset allows us to handle multithreaded stateful processing.
 * <p/>
 * In a multi-threaded environment such as a Servlet, multiple requests for the
 * same dataset may happen at the same time. This is a problem only when there is
 * some state that is being maintained. Caching strategies (eg the netcdf server
 * caches open netcdf files) may need to maintain state information in order to manage
 * resources efficiently.
 * <p/>
 * All accesses to the DDS and DAS are made through the GuardedDataset.
 * Typically the server puts a mutex lock on the resource when getDDS() or getDAS()
 * is called. When the dataset processing is complete, release() is called, and
 * the server releases the mutex.
 * <p/>
 * Example use:
 * <p/>
 * 
 * <pre>
 * <code>
 * public void doGetDAS(HttpServletRequest request,
 * HttpServletResponse response,
 * ReqState rs)
 * throws IOException, ServletException {
 * <p/>
 * response.setContentType("text/plain");
 * response.setHeader("XDODS-Server",  getServerVersion() );
 * response.setHeader("Content-Description", "dods-dds");
 * OutputStream Out = new BufferedOutputStream(response.getOutputStream());
 * <p/>
 * GuardedDataset ds = null;
 * try {
 * ds = getDataset(rs);
 * DAS myDAS = ds.getDAS();            // server would lock here
 * myDAS.print(Out);
 * response.setStatus(response.SC_OK);
 * }
 * catch (DAP2Exception de){
 * dap2ExceptionHandler(de,response);
 * }
 * catch (ParseException pe) {
 * parseExceptionHandler(pe,response);
 * }
 * finally {                           // release lock if needed
 * if (ds != null) ds.release();
 * }
 * }
 * </code>
 * </pre>
 * <p/>
 * Its important that the DDS or DAS not be used after release() is called.
 * <p/>
 * See opendap.servers.netcdf.NcDataset for example of implementing a locking
 * GuardedDataset.
 * If a server is not keeping state, it can simply pass the DDS and DAS without locking,
 * and implement a dummy release() method.
 *
 * @author jcaron
 */

public interface GuardedDataset extends Closeable {

  /**
   * Get the DDS for this Dataset.
   *
   * @return the ServerDDS
   * @throws opendap.dap.DAP2Exception
   * @throws ParseException
   */
  ServerDDS getDDS() throws opendap.dap.DAP2Exception, ParseException;

  /**
   * Get the DAS for this Dataset.
   *
   * @return the DAS
   */
  DAS getDAS() throws opendap.dap.DAP2Exception, ParseException;

  /**
   * Release the lock, if any, on this dataset.
   */
  void release();

  void close();
}


