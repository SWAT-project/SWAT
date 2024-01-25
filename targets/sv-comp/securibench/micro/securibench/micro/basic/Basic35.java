// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
/**
 * @author Benjamin Livshits <livshits@cs.stanford.edu>
 *     <p>$Id: Basic35.java,v 1.2 2006/04/04 20:00:40 livshits Exp $
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
 * @servlet description="values obtained from HttpServletRequest"
 * @servlet vuln_count = "6"
 */
public class Basic35 extends BasicTestCase implements MicroTestCase {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Enumeration e = req.getHeaderNames();
    while (e.hasMoreElements()) {
      PrintWriter writer = resp.getWriter();
      // I believe these can be forged also
      // TODO: double-check this
      writer.println(req.getProtocol()); /* BAD */
      writer.println(req.getScheme()); /* BAD */
      writer.println(req.getAuthType()); /* BAD */
      writer.println(req.getQueryString()); /* BAD */
      writer.println(req.getRemoteUser()); /* BAD */
      writer.println(req.getRequestURL()); /* BAD */
    }
  }

  public String getDescription() {
    return "values obtained from headers";
  }

  public int getVulnerabilityCount() {
    return 6;
  }
}
