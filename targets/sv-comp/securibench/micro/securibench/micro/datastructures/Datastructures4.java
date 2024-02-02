// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Datastructures4.java,v 1.1 2006/04/21 17:14:24 livshits Exp $

   changed by FH: c1.next.str is constant
*/
package securibench.micro.datastructures;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple nexted data (false positive)"
 * @servlet vuln_count = "0"
 */
public class Datastructures4 extends BasicTestCase implements MicroTestCase {
  public class C {
    private String str;
    private C next;

    public String getData() {
      return this.str;
    }

    public void setData(String str) {
      this.str = str;
    }

    public void setNext(C next) {
      this.next = next;
    }
  }

  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter(FIELD_NAME);
    C c1 = new C();
    c1.setData(name);

    C c2 = new C();
    c2.setData("abc");
    c1.setNext(c2);

    String str = c1.next.str;

    PrintWriter writer = resp.getWriter();
    writer.println(str); /* OK */
  }

  public String getDescription() {
    return "simple nexted data";
  }

  public int getVulnerabilityCount() {
    return 0;
  }
}
