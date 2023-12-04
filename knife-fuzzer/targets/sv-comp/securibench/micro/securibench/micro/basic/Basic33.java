// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
/**
 * @author Benjamin Livshits <livshits@cs.stanford.edu>
 *     <p>$Id: Basic33.java,v 1.2 2006/04/04 20:00:40 livshits Exp $
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
 * @servlet vuln_count = "1"
 */
public class Basic33 extends BasicTestCase implements MicroTestCase {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Enumeration e = req.getHeaders("Accept-Language");
    while (e.hasMoreElements()) {
      String header = (String) e.nextElement();

      PrintWriter writer = resp.getWriter();
      writer.println(header); /* BAD */
    }
  }

  public String getDescription() {
    return "values obtained from headers";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
