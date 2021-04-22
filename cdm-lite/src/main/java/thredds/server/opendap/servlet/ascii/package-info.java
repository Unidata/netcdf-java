/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 * This package contains the OPeNDAP ASCII client. These classes are used directly
 * (via the class factory asciiFactory) by the AbstractServlet class to provide the
 * ASCII response from a OPeNDAP Server. The class implementations contain the
 * logic for flattening the complex OPeNDAP data structures into columns of comma
 * delimited ascii text.
 */
package thredds.server.opendap.servlet.ascii;
