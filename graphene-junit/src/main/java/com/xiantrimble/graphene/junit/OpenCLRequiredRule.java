/**
 * Copyright (C) 2014 Christian Trimble (xiantrimble@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xiantrimble.graphene.junit;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

import java.util.function.Supplier;

/**
 * Skips a test if there are no OpenCL devices available.
 * 
 * @author Christian Trimble
 *
 */
public class OpenCLRequiredRule implements TestRule {
  public static boolean openClAvailable = false;

  static {
    try {
      PLATFORM: for (CLPlatform platform : JavaCL.listPlatforms()) {
        if (platform.listAllDevices(true).length > 0) {
          openClAvailable = true;
          break PLATFORM;
        }
      }
    } catch (UnsatisfiedLinkError ule) {
      System.err.println("OpenCL not available.");
    }
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        if (!openClAvailable) {
          throw new AssumptionViolatedException("OpenCL not available.");
        }
        base.evaluate();
      }
    };
  }
}
