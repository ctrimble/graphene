package com.xiantrimble.graphene;

import java.nio.ByteOrder;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.bridj.Pointer;

import com.google.common.collect.Lists;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLProgram;
import com.xiantrimble.graphene.Graphene.GrapheneDeviceContext;

import static com.xiantrimble.graphene.JavaCLUtils.releaseQuietly;
import static com.xiantrimble.graphene.JavaCLUtils.waitForAndRelease;

public class GrapheneTestTemplates {
	
	public static interface Assertions {
		public void accept( List<Argument<?>> arguments ) throws Exception;
	}
	
	public static class Argument<T> implements AutoCloseable {
		Argument( Usage usage, Function<ByteOrder, Pointer<T>> initFunction ) {
			this.usage = usage;
			this.initFunction = initFunction;
		}
		Function<ByteOrder, Pointer<T>> initFunction;
		Usage usage;
		Pointer<T> pointer;
		CLBuffer<T> buffer;
		
		public CLBuffer<T> getBuffer() { return buffer; }
		
		@SuppressWarnings({ "unchecked" })
    private void init(GrapheneDeviceContext deviceContext) {
			pointer = initFunction.apply(deviceContext.getByteOrder());
			buffer = (CLBuffer<T>) deviceContext.getContext().createBuffer(usage, (Class<?>)pointer.getTargetType(), pointer.getValidElements());
      waitForAndRelease(buffer.write(deviceContext.getQueue(), 0, pointer.getValidElements(), pointer, true));    
		}
		
		private void readData(GrapheneDeviceContext deviceContext) {
			if( usage == Usage.InputOutput || usage == Usage.Output ) {
				waitForAndRelease(buffer.read(deviceContext.getQueue(), 0, pointer.getValidElements(), pointer, true));
			}  			
		}
		
		@Override
    public void close() {
			releaseQuietly(buffer);
			releaseQuietly(pointer);
    }
	}
	
	public static <T> Supplier<T> constantSupplier( T value ) {
		return ()->value;
	}

  public static class MultiArgument {
  	protected Supplier<Graphene> grapheneSupplier;
  	protected List<Argument<?>> arguments = Lists.newArrayList();
  	protected Supplier<String[]> sourcesSupplier;
  	protected Supplier<String> kernelNameSupplier;
  	protected Supplier<int[]> globalWorkSizeSupplier;
  	protected Consumer<List<Argument<?>>> validator;
  	protected Assertions assertions;
  	
  	public MultiArgument withGraphene(Supplier<Graphene> grapheneSupplier) {
  		this.grapheneSupplier = grapheneSupplier;
  		return this;
  	}
  	
  	public MultiArgument withSourceSupplier(Supplier<String[]> sourcesSupplier) {
  		this.sourcesSupplier = sourcesSupplier;
  		return this;
  	}
  	
  	public MultiArgument withKernelNameSupplier(Supplier<String> kernelNameSupplier) {
  		this.kernelNameSupplier = kernelNameSupplier;
  		return this;
  	}
  	
  	public MultiArgument withGlobalWorkSizeSupplier(Supplier<int[]> globalWorkSizeSupplier) {
  		this.globalWorkSizeSupplier = globalWorkSizeSupplier;
  		return this;
  	}

  	public <T> MultiArgument addArgument( Usage usage, Function<ByteOrder, Pointer<T>> initFunction ) {
  		arguments.add(new Argument<T>(usage, initFunction));
  		return this;
  	}
  	
  	public MultiArgument withAssertions( Assertions assertions ) {
  		this.assertions = assertions;
  		return this;
  	}
  	
		public void doTest() throws Exception {
			CLProgram program = null;
			CLKernel kernel = null;

			try {
				Graphene graphene = grapheneSupplier.get();

				GrapheneDeviceContext deviceContext = graphene.getContexts().get(0);

				arguments.forEach(a -> a.init(deviceContext));

				program = deviceContext.getContext().createProgram(
				    sourcesSupplier.get());

				kernel = program.createKernel(kernelNameSupplier.get(), arguments
				    .stream().map(Argument::getBuffer).toArray());
				waitForAndRelease(kernel.enqueueNDRange(deviceContext.getQueue(),
						globalWorkSizeSupplier.get()));
				
				arguments.forEach(a -> a.readData(deviceContext));

				assertions.accept(arguments);
			} finally {
				// call function to validate output.
				releaseQuietly(kernel);
				releaseQuietly(program);
				arguments.forEach(Argument::close);
			}

		}
  }
}
