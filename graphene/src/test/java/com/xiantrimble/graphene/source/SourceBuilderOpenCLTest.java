package com.xiantrimble.graphene.source;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assert.fail;

import java.nio.ByteOrder;

import org.bridj.Pointer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.util.IOUtils;
import com.xiantrimble.graphene.AbstractStructTest;
import com.xiantrimble.graphene.CombinatoricTest.BinomialCoefficientContext;
import com.xiantrimble.graphene.Graphene;
import com.xiantrimble.graphene.Graphene.ComputationContext;
import com.xiantrimble.graphene.Graphene.ComputationContext.DeviceComputationContext;
import com.xiantrimble.graphene.Graphene.GrapheneDeviceContext;
import com.xiantrimble.graphene.junit.GrapheneRule;
import com.xiantrimble.graphene.junit.GrapheneRules;

/**
 * Tests related to using the output of the source builder with
 * OpenCL.
 * 
 * @author Christian Trimble
 *
 */
public class SourceBuilderOpenCLTest {
  public static @ClassRule GrapheneRule grapheneSupplier = GrapheneRules.graphene();
  
  SourcesBuilder sourceBuilder;
  
  @Before
  public void setUp() {
  	sourceBuilder = SourcesBuilder.builder()
  			.withBaseUri("resource:/")
  			.withIncludeResolver(StaticIncludeResolver.builder().build())
  			.build();
  }
  

	@Test
	public void shouldLoadValidKernelFromSource() {
		String[] source = sourceBuilder.addSource(
				"kernel void add( global int* arg1, global int* arg2, global int* output)\n"+
         "{\n"+ 
				 "  int index = get_global_id(0);\n"+
         "  output[index] = arg1[index] + arg2[index];\n"+
         "}\n")
         .build();
     
		int result = doComputation(source, "add", 1, 2);

		assertThat(result, equalTo(3));
	}
	
	/**
	 * This test was removed because the line format is not predictable
	 * between vendors.  Perhaps a JUnit rule could isolate this and make it
	 * useful.
	 */
	@Ignore
	@Test
	public void shouldReportLineNumberAndUrlFromSource() {
		String[] source = sourceBuilder.addSource(
				"kernel void add( global int* arg1, global int* arg2, global int* output)\n"+
         "{\n"+ 
				 "  int index = get_global_id(0)\n"+
         "  output[index] = arg1[index] + arg2[index];\n"+
         "}\n")
         .build();
     
		try {
		  doComputation(source, "add", 1, 2);
		  fail("Exception not thrown.");
		}
		catch( CLBuildException e ) {
			// This would be better as sources:index_0
			assertThat(e.getMessage(), containsString("source:0:3:"));
		}
		
	}
	
	public static int doComputation( String[] sources, String kernel, Integer arg1, Integer arg2 ) {
		
		Graphene graphene = grapheneSupplier.get();
		try( ComputationContext computation = graphene.createComputationContext() ) {
    DeviceComputationContext deviceContext = computation.getDeviceComputationContexts().get(0);
    ByteOrder byteOrder = deviceContext.getDeviceContext().getByteOrder();

    Pointer<Integer> arg1Pointer = Pointer.allocateInt().order(byteOrder).setInt(arg1);
    Pointer<Integer> arg2Pointer = Pointer.allocateInt().order(byteOrder).setInt(arg2);
    Pointer<Integer> resultPointer = Pointer.allocateInt().order(byteOrder);

    CLBuffer<Integer> arg1Buffer = deviceContext.createBuffer(Usage.Input, arg1Pointer);
    CLBuffer<Integer> arg2Buffer = deviceContext.createBuffer(Usage.Output, arg2Pointer);
    CLBuffer<Integer> resultBuffer = deviceContext.createBuffer(Usage.Output, resultPointer);
    
    CLProgram program = deviceContext.createProgram(sources);

    CLKernel expandKernel = program.createKernel(kernel);
    expandKernel.setArgs(arg1Buffer, arg2Buffer, resultBuffer);

    CLEvent kernelEvent = expandKernel.enqueueNDRange(deviceContext.getDeviceContext().getQueue(), new int[] { 1 });

    resultPointer = resultBuffer.read(deviceContext.getDeviceContext().getQueue(), kernelEvent);

    return resultPointer.get();
		}
	}
}
