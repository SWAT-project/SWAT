// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2013 Alexander von Rhein, University of Passau
// SPDX-FileCopyrightText: 2011-2021 The SV-Benchmarks Community
//
// SPDX-License-Identifier: Apache-2.0

import MinePumpSystem.Environment;
import MinePumpSystem.MinePump;

public class Actions {

  Environment env;
  MinePump p;

  boolean methAndRunningLastTime = false;
  boolean switchedOnBeforeTS = false;

  Actions() {
    env = new Environment();
    p = new MinePump(env);
  }

  void waterRise() {
    env.waterRise();
  }

  void methaneChange() {
    env.changeMethaneLevel();
  }

  void stopSystem() {
    if (p.isSystemActive()) p.stopSystem();
  }

  void startSystem() {
    if (!p.isSystemActive()) p.startSystem();
  }

  void timeShift() {

    if (p.isSystemActive()) Specification5_1();

    p.timeShift();

    if (p.isSystemActive()) {
      Specification1();
      Specification2();
      Specification3();
      Specification4();
      Specification5_2();
    }
  }

  String getSystemState() {
    return p.toString();
  }

  // Specification 1 methan is Critical and pumping leads to Error
  void Specification1() {

    Environment e = p.getEnv();

    boolean b1 = e.isMethaneLevelCritical();
    boolean b2 = p.isPumpRunning();

    if (b1 && b2) {
      assert false;
    }
  }

  // Specification 2: When the pump is running, and there is methane, then it is
  // in switched off at most 1 timesteps.
  void Specification2() {

    Environment e = p.getEnv();

    boolean b1 = e.isMethaneLevelCritical();
    boolean b2 = p.isPumpRunning();

    if (b1 && b2) {
      if (methAndRunningLastTime) {
        assert false;
      } else {
        methAndRunningLastTime = true;
      }
    } else {
      methAndRunningLastTime = false;
    }
  }

  // Specification 3: When the water is high and there is no methane, then the
  // pump is on.
  void Specification3() {

    Environment e = p.getEnv();

    boolean b1 = e.isMethaneLevelCritical();
    boolean b2 = p.isPumpRunning();
    boolean b3 = e.getWaterLevel() == Environment.WaterLevelEnum.high;

    if (!b1 && b3 && !b2) {
      assert false;
    }
  }

  // Specification 4: the pump is never on when the water level is low
  void Specification4() {

    Environment e = p.getEnv();

    boolean b2 = p.isPumpRunning();
    boolean b3 = e.getWaterLevel() == Environment.WaterLevelEnum.low;

    if (b3 && b2) {
      assert false;
    }
  }

  // Specification 5: The Pump is never switched on when the water is below the
  // highWater sensor.
  void Specification5_1() {
    switchedOnBeforeTS = p.isPumpRunning();
  }

  // Specification 5: The Pump is never switched on when the water is below the
  // highWater sensor.
  void Specification5_2() {

    Environment e = p.getEnv();

    boolean b1 = p.isPumpRunning();
    boolean b2 = e.getWaterLevel() != Environment.WaterLevelEnum.high;

    if ((b2) && (b1 && !switchedOnBeforeTS)) {
      assert false;
    }
  }
}
