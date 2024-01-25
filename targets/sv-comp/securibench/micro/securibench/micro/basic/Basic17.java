// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Basic17.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple heap-allocated data strucure"
 * @servlet vuln_count = "1"
 */
public class Basic17 extends BasicTestCase implements MicroTestCase {
  public class Widget {
    String contents;

    public String getContents() {
      return contents;
    }

    public void setContents(String contents) {
      this.contents = contents;
    }
  }

  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s = req.getParameter(FIELD_NAME);
    Widget w1 = new Widget();
    w1.setContents(s);

    Widget w2 = new Widget();
    w2.setContents("abc");

    PrintWriter writer = resp.getWriter();
    writer.println(w1.getContents()); /* BAD */
    writer.println(w2.getContents()); /* OK */
  }

  public String getDescription() {
    return "simple heap-allocated data strucure";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
