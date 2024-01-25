// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Collections3.java,v 1.5 2006/04/04 20:00:41 livshits Exp $
*/
package securibench.micro.collections;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description = "collection of collections"
 * @servlet vuln_count = "1"
 */
public class Collections3 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s1 = req.getParameter(FIELD_NAME);
    LinkedList ll1 = new LinkedList();
    LinkedList ll2 = new LinkedList();
    ll1.addLast(s1); // FH: changed to make vulnerable
    ll2.addLast(ll1);

    LinkedList c = (LinkedList) ll2.getLast();
    String s2 = (String) c.getLast();

    PrintWriter writer = resp.getWriter();
    writer.println(s2); /* BAD */
    // this is because the print out of c includes the test of s1
    // FH: in the SVCOMP mock this is not bad
  }

  public String getDescription() {
    return "collection of collections";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
