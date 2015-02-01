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
    PLATFORM: for( CLPlatform platform : JavaCL.listPlatforms() ) {
    	if( platform.listAllDevices(true).length > 0 ) {
    		openClAvailable = true;
    		break PLATFORM;
    	}
    }
	}
	
	@Override
  public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
      public void evaluate() throws Throwable {
				if( !openClAvailable ) {
					throw new AssumptionViolatedException("OpenCL not available.");
				}
	      base.evaluate();
      }
		};
  } 
}
