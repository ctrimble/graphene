package com.xiantrimble.graphene;

import java.util.stream.IntStream;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;
import org.junit.ClassRule;
import org.junit.Test;

import com.nativelibs4java.opencl.CLMem.Usage;
import com.xiantrimble.graphene.junit.GrapheneRule;
import com.xiantrimble.graphene.junit.GrapheneRules;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;
import static com.xiantrimble.graphene.GrapheneTestTemplates.*;
import static java.lang.String.format;

public class MultiArgumentTest {
  public static @ClassRule GrapheneRule grapheneSupplier = GrapheneRules.graphene();

  @Test
	public void shouldSupportPrimativeArguments() throws Exception {
  	int count = 20;
		new MultiArgument()
		  .withGraphene(grapheneSupplier)
		  .withSourceSupplier(()->{
		  	return new String[]{
		  			"kernel void multiply( global int* input, global int* output ) {" +
		  	    "  int index = get_global_id(0);"+
		  		  "  output[index] = input[index]*2;" +
		  	    "}"
		  	};
		  })
		  .withKernelNameSupplier(constantSupplier("multiply"))
		  .withGlobalWorkSizeSupplier(constantSupplier(new int[] {count}))
		  .addArgument(Usage.Input, (b)->{
		  	return Pointer
		  			.allocateInts(count)
		  			.order(b)
		  			.setArray(IntStream.range(0, count).toArray());
		  })
		  .addArgument(Usage.Output, (b)->{
		  	return Pointer.allocateInts(count).order(b);
		  })
		  .withAssertions((arguments)->{
		  	@SuppressWarnings("unchecked")
        Argument<Integer> argument = (Argument<Integer>)arguments.get(1);
		  	
		  	for( int i = 0; i < count; i++ ) {
		  		assertThat(format("index %d is correct", i), argument.pointer.get(i).intValue(), equalTo(i*2));
		  	}
		  })
		  .doTest();
	}
  
  public static class InputStruct extends StructObject {
  	public static final int VALUE_INDEX = 0;
  	
  	@Field(VALUE_INDEX)
  	public InputStruct value(int value) {
  		this.io.setIntField(this, VALUE_INDEX, value);
  		return this;
  	}
  	
  	@Field(VALUE_INDEX)
  	public int value() {
  		return this.io.getIntField(this, VALUE_INDEX);
  	}
  }
  
  
  public static class OutputStruct extends StructObject {
  	public static final int VALUE_INDEX = 0;
  	
  	@Field(VALUE_INDEX)
  	public OutputStruct value(int value) {
  		this.io.setIntField(this, VALUE_INDEX, value);
  		return this;
  	}
  	
  	@Field(VALUE_INDEX)
  	public int value() {
  		return this.io.getIntField(this, VALUE_INDEX);
  	}
  }

  
  @Test
  public void shouldSupportArrayStructures() throws Exception {
  	int count = 10;
		new MultiArgument()
	  .withGraphene(grapheneSupplier)
	  .withSourceSupplier(()->{
	  	return new String[]{
	  			"typedef struct { int value; } InputStruct;" +
	  	    "typedef struct { int value; } OutputStruct; " +
	  			"kernel void multiply( global InputStruct* input, global OutputStruct* output ) {" +
	  	    "  int index = get_global_id(0);"+
	  		  "  output[index].value = input[index].value*2;" +
	  	    "}"
	  	};
	  })
	  .withKernelNameSupplier(constantSupplier("multiply"))
	  .withGlobalWorkSizeSupplier(constantSupplier(new int[] {count}))
	  .addArgument(Usage.Input, (b)->{
	  	return Pointer
	  			.allocateArray(InputStruct.class, count)
	  			.order(b)
	  			.setArray(IntStream.range(0, count).mapToObj(i->new InputStruct().value(i)).toArray());
	  })
	  .addArgument(Usage.Output, (b)->{
	  	return Pointer.allocateArray(OutputStruct.class, count).order(b);
	  })
	  .withAssertions((arguments)->{
	  	@SuppressWarnings("unchecked")
      Argument<OutputStruct> argument = (Argument<OutputStruct>)arguments.get(1);
	  	
	  	for( int i = 0; i < count; i++ ) {
	  		assertThat(format("index %d is correct", i), argument.pointer.get(i).value(), equalTo(i*2));
	  	}
	  })
	  .doTest();
  }
  
  @Test
  public void shouldSupportIndividualStructures() throws Exception {
		new MultiArgument()
	  .withGraphene(grapheneSupplier)
	  .withSourceSupplier(()->{
	  	return new String[]{
	  			"typedef struct { int value; } InputStruct;" +
	  	    "typedef struct { int value; } OutputStruct; " +
	  			"kernel void multiply( global InputStruct* input, global OutputStruct* output ) {" +
	  		  "  output->value = input->value*2;" +
	  	    "}"
	  	};
	  })
	  .withKernelNameSupplier(constantSupplier("multiply"))
	  .withGlobalWorkSizeSupplier(constantSupplier(new int[] {1}))
	  .addArgument(Usage.Input, (b)->{
	  	Pointer<InputStruct> pointer = Pointer
	  			.allocate(InputStruct.class)
	  			.order(b);
	  	pointer.set(new InputStruct().value(1));
	  	return pointer;
	  })
	  .addArgument(Usage.Output, (b)->{
	  	return Pointer.allocate(OutputStruct.class).order(b);
	  })
	  .withAssertions((arguments)->{
	  	@SuppressWarnings("unchecked")
      Argument<OutputStruct> argument = (Argument<OutputStruct>)arguments.get(1);
	  	assertThat("the value is correct", argument.pointer.get().value(), equalTo(2));
	  })
	  .doTest();  	
  }
}
