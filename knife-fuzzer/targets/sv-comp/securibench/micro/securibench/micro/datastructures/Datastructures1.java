// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Datastructures1.java,v 1.1 2006/04/21 17:14:24 livshits Exp $
*/
package securibench.micro.datastructures;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple test of field assignment"
 * @servlet vuln_count = "1"
 */
public class Datastructures1 extends BasicTestCase implements MicroTestCase {
  public class C {
    private String str;
    private String tag = "abc";

    public String getData() {
      return this.str;
    }

    public String getTag() {
      return this.str;
    }

    public void setData(String str) {
      this.str = str;
    }
  }

  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter(FIELD_NAME);
    C c = new C();
    c.setData(name);
    String str = c.getData();
    String tag = c.getTag();

    PrintWriter writer = resp.getWriter();
    writer.println(str); /* BAD */
    writer.println(tag); /* OK */
  }

  public String getDescription() {
    return "simple test of field assignment";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
