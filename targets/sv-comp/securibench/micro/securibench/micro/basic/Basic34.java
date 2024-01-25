// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
/**
 * @author Benjamin Livshits <livshits@cs.stanford.edu>
 *     <p>$Id: Basic34.java,v 1.2 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="values obtained from headers"
 * @servlet vuln_count = "2"
 */
public class Basic34 extends BasicTestCase implements MicroTestCase {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Enumeration e = req.getHeaderNames();
    while (e.hasMoreElements()) {
      String headerName = (String) e.nextElement();
      String headerValue = (String) req.getHeader(headerName);

      PrintWriter writer = resp.getWriter();
      // I believe arbitrary header names can be forged
      // TODO: double-check this
      writer.println(headerName); /* BAD */
      writer.println(headerValue); /* BAD */
    }
  }

  public String getDescription() {
    return "values obtained from headers";
  }

  public int getVulnerabilityCount() {
    return 2;
  }
}
