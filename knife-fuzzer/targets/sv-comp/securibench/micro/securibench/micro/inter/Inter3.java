// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Inter3.java,v 1.6 2006/04/21 17:14:26 livshits Exp $
*/
package securibench.micro.inter;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="chains of method calls"
 * @servlet vuln_count = "1"
 */
public class Inter3 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";
  private PrintWriter writer;

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter(FIELD_NAME);

    writer = resp.getWriter();
    f1(name);
  }

  private void f1(String name) {
    f2(name);
  }

  private void f2(String name) {
    f3(name);
    f3("ade".concat(name));
  }

  private void f3(String name) {
    f4(name);
  }

  private void f4(String name) {
    f5(name);
  }

  private void f5(String name) {
    f6(name);
  }

  private void f6(String name) {
    f7(name);
    f7(name + "abc");
    f8("adsf " + name + "abc");
    f8("adsf " + name + "abc");
  }

  private void f7(String name) {
    f8(name);
  }

  private void f8(String name) {
    f9(name);
  }

  // reachable code
  private void f9(String name) {
    writer.println(name); /* BAD */
  }

  // dead code
  public void f0(String name) {
    writer.println(name); /* OK */
  }

  public String id(String string, PrintWriter writer) {
    writer.println(string); /* OK */

    return string;
  }

  public String getDescription() {
    return "chains of method calls";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
