// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: StrongUpdates4.java,v 1.2 2006/04/04 20:00:41 livshits Exp $

   // FH: changed verdict, this is only vulnerable with additional assumptions
   // about execution environment

*/
package securibench.micro.strong_updates;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="a more tricky test of when we can't assume a strong update with multiple
 *     variables that are not thread-local"
 * @servlet vuln_count = "0"
 */
public class StrongUpdates4 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";
  /* This is really tricky because the field is shared across multiple users of the same servlet.
   * So, depending on the user interaction, we can have a data race with two users accessing field
   * "name". Therefore, when u1 sets it, u1 resets it, u2 sets it, u1 reads it, we can still have a problem.
   **/
  private String name;

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    name = req.getParameter(FIELD_NAME);
    name = "abc";

    PrintWriter writer = resp.getWriter();
    writer.println(name); /* BAD */
  }

  public String getDescription() {
    return "a more tricky test of when we can't assume a strong "
        + "update with multiple variables that are not thread-local";
  }

  public int getVulnerabilityCount() {
    return 0;
  }
}
