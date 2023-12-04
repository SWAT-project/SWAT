// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Aliasing3.java,v 1.1 2006/04/21 17:14:27 livshits Exp $

   changed by Falk Howar: no vulnerability
*/
package securibench.micro.aliasing;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple aliasing of an array element"
 * @servlet vuln_count = "0"
 */
public class Aliasing3 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter(FIELD_NAME);
    String[] a = new String[10];
    String str = a[5];
    a[5] = name;
    name = str;

    PrintWriter writer = resp.getWriter();
    writer.println(str); /* BAD */
  }

  public String getDescription() {
    return "simple aliasing of an array element";
  }

  public int getVulnerabilityCount() {
    return 0;
  }
}
