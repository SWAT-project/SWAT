// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Factories3.java,v 1.3 2006/04/04 20:00:41 livshits Exp $
*/
package securibench.micro.factories;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description = "factory problem with a string wrapper"
 * @servlet vuln_count = "1"
 */
public class Factories3 extends BasicTestCase implements MicroTestCase {
  class StringWrapper {
    StringWrapper(String value) {
      this.value = value;
    }

    public String toString() {
      return value;
    }

    protected String value;
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s1 = req.getParameter("name");

    StringWrapper w1 = new StringWrapper(s1);
    StringWrapper w2 = new StringWrapper("abc");

    PrintWriter writer = resp.getWriter();

    writer.println(w1.toString()); /* BAD */
    writer.println(w2.toString()); /* OK */
  }

  public String getDescription() {
    return "factory problem with a string wrapper";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
