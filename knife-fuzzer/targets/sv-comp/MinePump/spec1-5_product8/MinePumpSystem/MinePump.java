// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2013 Alexander von Rhein, University of Passau
// SPDX-FileCopyrightText: 2011-2021 The SV-Benchmarks Community
//
// SPDX-License-Identifier: Apache-2.0

package MinePumpSystem;

public class MinePump {

  boolean pumpRunning = false;

  boolean systemActive = true;

  Environment env;

  public MinePump(Environment env) {
    super();
    this.env = env;
  }

  public void timeShift() {
    if (pumpRunning) env.lowerWaterLevel();
    if (systemActive) processEnvironment();
  }

  private void processEnvironment__wrappee__base() {}

  public void processEnvironment() {
    if (pumpRunning && isMethaneAlarm()) {
      deactivatePump();
    } else {
      processEnvironment__wrappee__base();
    }
  }

  void activatePump() {
    pumpRunning = true;
  }

  public boolean isPumpRunning() {
    return pumpRunning;
  }

  void deactivatePump() {
    pumpRunning = false;
  }

  boolean isMethaneAlarm() {
    return env.isMethaneLevelCritical();
  }

  @Override
  public String toString() {
    return "Pump(System:"
        + (systemActive ? "On" : "Off")
        + ",Pump:"
        + (pumpRunning ? "On" : "Off")
        + ") "
        + env.toString();
  }

  public Environment getEnv() {
    return env;
  }

  public void stopSystem() {
    if (pumpRunning) {
      deactivatePump();
    }
    assert !pumpRunning;
    systemActive = false;
  }

  public void startSystem() {
    assert !pumpRunning;
    systemActive = true;
  }

  public boolean isSystemActive() {
    return systemActive;
  }
}
