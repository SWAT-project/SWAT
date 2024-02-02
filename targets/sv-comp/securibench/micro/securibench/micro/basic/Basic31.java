// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
/**
 * @author Benjamin Livshits <livshits@cs.stanford.edu>
 *     <p>$Id: Basic31.java,v 1.2 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.Cookie;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="values obtained from cookies"
 * @servlet vuln_count = "2"
 */
public class Basic31 extends BasicTestCase implements MicroTestCase {

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Cookie[] cookies = req.getCookies();

    String name = cookies[0].getName();
    String value = cookies[0].getValue();

    PrintWriter writer = resp.getWriter();

    if (name != null) {
      writer.println(name); /* BAD */
    }
    if (value != null) {
      writer.println(value); /* BAD */
    }
  }

  public String getDescription() {
    return "values obtained from cookies";
  }

  public int getVulnerabilityCount() {
    return 2;
  }
}
