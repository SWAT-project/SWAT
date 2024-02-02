// SPDX-FileCopyrightText: 2021 Falk Howar falk.howar@tu-dortmund.de
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

import java.io.IOException;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import org.sosy_lab.sv_benchmarks.Verifier;
import securibench.micro.reflection.Refl3;

public class Main {

  public static void main(String[] args) {
    String s1 = Verifier.nondetString();
    HttpServletRequest req = new HttpServletRequest();
    HttpServletResponse res = new HttpServletResponse();
    req.setTaintedValue(s1);

    Refl3 sut = new Refl3();
    try {
      sut.doGet(req, res);
    } catch (IOException e) {

    }
  }
}
