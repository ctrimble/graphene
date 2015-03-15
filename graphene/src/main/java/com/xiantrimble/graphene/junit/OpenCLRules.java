package com.xiantrimble.graphene.junit;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;
import com.xiantrimble.graphene.JavaCLUtils;

public class OpenCLRules {
  public static boolean openClAvailable = false;

  static {
    try {
      for (CLPlatform platform : JavaCL.listPlatforms()) {
        if (!openClAvailable && platform.listAllDevices(true).length > 0) {
          openClAvailable = true;
        }
        JavaCLUtils.releaseQuietly(platform);
      }
    } catch (UnsatisfiedLinkError ule) {
      System.err.println("OpenCL not available.");
    }
  }
  
  public static void assertOpenCLAvailable() {
    if (!openClAvailable) {
      throw new AssumptionViolatedException("OpenCL not available.");
    }  	
  }
  
  /**
   * A test rule that skips tests if OpenCL is not available.
   * 
   * @return
   */
  public static TestRule assumeAvailable() {
  	return new TestRule() {
    @Override
    public Statement apply(Statement base, Description description) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
        	assertOpenCLAvailable();
          base.evaluate();
        }
      };
    }
  	};
  }
}
