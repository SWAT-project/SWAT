// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: StrongUpdates5.java,v 1.3 2006/04/21 17:14:27 livshits Exp $
*/
package securibench.micro.strong_updates;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="making a shared servlet field thread-local"
 * @servlet vuln_count = "0"
 */
public class StrongUpdates5 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";
  private String name;

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // access to this.name is protected within the block, so we are safe
    synchronized (this.name) {
      name = req.getParameter(FIELD_NAME);
      name = "abc";

      PrintWriter writer = resp.getWriter();
      writer.println(name); /* OK */
    }
  }

  public String getDescription() {
    return "making a shared servlet field thread-local";
  }

  public int getVulnerabilityCount() {
    return 0;
  }
}
