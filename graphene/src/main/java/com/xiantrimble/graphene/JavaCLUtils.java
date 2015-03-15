package com.xiantrimble.graphene;

import static com.xiantrimble.graphene.JavaCLUtils.releaseQuietly;

import java.util.List;

import org.bridj.Pointer;

import com.google.common.collect.Lists;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

public class JavaCLUtils {
	
	public static List<CLDevice> devices() {
		List<CLDevice> devices = Lists.newArrayList();
		for (CLPlatform platform : JavaCL.listPlatforms()) {
			for( CLDevice device : platform.listAllDevices(true)) {
			  devices.add(device);
			}
      //releaseQuietly(platform);
		}
		return devices;
	}

	public static void releaseQuietly(CLPlatform platform) {
		if( platform != null ) {
			try {
				platform.release();
			}
			catch( Exception e ) {
				// TODO: logging.
			}
		}
  }

	public static void releaseQuietly(CLQueue queue) {
		if( queue != null ) {
			try {
	  	  queue.release();
			}
			catch( Exception e ) {
				// TODO: logging.
			}
		}
  }

	public static void releaseQuietly(CLContext context) {
		if( context != null ) {
			try {
				context.release();
			}
			catch( Exception e ) {
				// TODO: logging.
			}
		}
  }

	public static void releaseQuietly(CLDevice device) {
		if( device != null ) {
			try {
				device.release();
			}
			catch( Exception e ) {
				// TODO: logging.
			}
		}
  }

	public static void releaseQuietly(CLProgram program) {
		if( program != null ) {
			try {
				program.release();
			}
			catch( Exception e ) {
				// TODO: logging.
			}
		}
  }

	public static <T> void releaseQuietly(CLBuffer<T> buffer) {
		if( buffer != null ) {
			try {
				buffer.release();
			}
			catch( Exception e ) {
				// TODO: logging.
			}
		}
  }
	/**
	 * Creates a context on a specific device.
	 * @param device
	 * @return
	 */
	public static CLContext createContext(CLDevice device) {
		return device.getPlatform().createContext(null, device);
  }

	public static void releaseQuietly(CLEvent event) {
		if( event != null ) {
			try {
				event.release();
			}
			catch( Exception e ) {
				// TODO: logging.
			}
		}
  }
	
	public static void releaseQuietly(CLKernel kernel) {
		if( kernel != null ) {
			try {
				kernel.release();
			}
			catch( Exception e ) {
				// TODO: logging.
			}
		}
  }

	public static void releaseQuietly(Pointer<?> pointer) {
		if( pointer != null ) {
			try {
				pointer.release();
			}
			catch( Exception e ) {
				// TODO: logging.
			}
		}
  }
	
	public static void waitForAndRelease( CLEvent event ) {
		try {
		  CLEvent.waitFor(event);
		}
		finally {
		  releaseQuietly(event);
		}
  }

}
