package com.xiantrimble.graphene.example.commands;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.bridj.Pointer;

import com.google.common.collect.Range;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.util.IOUtils;
import com.xiantrimble.graphene.Ranges;

public class CombinatoricEnumerator<E> {
  public static class Builder<E> {
    protected String[] elements;
    protected int k;
    protected EventFactory<E> eventFactory;
    protected BiConsumer<E, Range<Long>> rangeSetter;
    protected Function<E, Range<Long>> rangeGetter;
    protected BiConsumer<E, Pointer<Integer>> elementSetter;
    protected Function<E, Pointer<Integer>> elementGetter;
    protected ExceptionHandler exceptionHandler = new ExceptionHandler() {

			@Override
      public void handleEventException(Throwable arg0, long arg1, Object arg2) {
        arg0.printStackTrace();
      }

			@Override
      public void handleOnShutdownException(Throwable arg0) {
        arg0.printStackTrace();
      }

			@Override
      public void handleOnStartException(Throwable arg0) {
        arg0.printStackTrace();
      }
    };

    public CombinatoricEnumerator.Builder<E> withElements(String... elements) {
      this.elements = elements;
      return this;
    }

    public CombinatoricEnumerator.Builder<E> withK(int k) {
      this.k = k;
      return this;
    }
    
    public CombinatoricEnumerator.Builder<E> withEventFactory(EventFactory<E> eventFactory) {
    	this.eventFactory = eventFactory;
      return this;
    }
    
    public CombinatoricEnumerator.Builder<E> withRangeSetter(BiConsumer<E, Range<Long>> rangeSetter) {
    	this.rangeSetter = rangeSetter;
      return this;
    }
    
    public CombinatoricEnumerator.Builder<E> withRangeGetter(Function<E, Range<Long>> rangeGetter) {
    	this.rangeGetter = rangeGetter;
      return this;
    }
    
    public CombinatoricEnumerator.Builder<E> withElementSetter(BiConsumer<E, Pointer<Integer>> elementSetter) {
    	this.elementSetter = elementSetter;
      return this;
    }
    
    public CombinatoricEnumerator.Builder<E> withElementGetter(Function<E, Pointer<Integer>> elementGetter) {
    	this.elementGetter = elementGetter;
      return this;
    }
    
    public CombinatoricEnumerator.Builder<E> withExceptionHandler(ExceptionHandler exceptionHandler) {
    	this.exceptionHandler = exceptionHandler;
      return this;
    }

    public CombinatoricEnumerator<E> build() {
      return new CombinatoricEnumerator<E>(eventFactory, rangeSetter, rangeGetter, elementSetter, elementGetter, exceptionHandler, elements, k);
    }
  }

  public static <E> Builder<E> builder() {
    return new Builder<E>();
  }

  protected String[] elements;
  protected int k;
  protected EventFactory<E> eventFactory;
  protected ExceptionHandler exceptionHandler;
  protected BiConsumer<E, Range<Long>> rangeSetter;
  protected Function<E, Range<Long>> rangeGetter;
  protected BiConsumer<E, Pointer<Integer>> elementSetter;
  protected Function<E, Pointer<Integer>> elementGetter;

  protected CombinatoricEnumerator(
  		EventFactory<E> eventFactory,
  	  BiConsumer<E, Range<Long>> rangeSetter,
  	  Function<E, Range<Long>> rangeGetter,
  	  BiConsumer<E, Pointer<Integer>> elementSetter,
  	  Function<E, Pointer<Integer>> elementGetter,
      ExceptionHandler exceptionHandler,
      String[] elements,
      int k) {
  	this.eventFactory = eventFactory;
  	this.rangeSetter = rangeSetter;
  	this.rangeGetter = rangeGetter;
  	this.elementSetter = elementSetter;
  	this.elementGetter = elementGetter;
  	this.exceptionHandler = exceptionHandler;
    this.elements = elements;
    this.k = k;
  }

	@SuppressWarnings("unchecked")
  public void enumerate(EventHandler<E> handler) throws InterruptedException, UnsupportedEncodingException {
    ExecutorService executor = Executors.newCachedThreadPool();
    Disruptor<E> disruptor =
        new Disruptor<E>(eventFactory, 128, executor);

    disruptor.handleExceptionsWith(exceptionHandler);
    CLContext context = JavaCL.createBestContext();
    CLQueue queue = context.createDefaultQueue();
    ByteOrder byteOrder = context.getByteOrder();

    int batchSize = 10000;

    // load the programs.
    CLProgram expandRange = context.createProgram(expandRangeProgram());

    disruptor.handleEventsWithWorkerPool(
        CombinationsCommand.<E> createPool(
            60,
            i -> {
              return e -> {
              	//System.out.println(k);
              	Range<Long> range = rangeGetter.apply(e);
                RangeCombinatoricContext computeContext =
                    new RangeCombinatoricContext()
                      .range(new IndexRange()
                        .start(range.lowerEndpoint())
                        .end(range.upperEndpoint()))
                      .uniqueElements(elements.length)
                      .subsetSize(k);

                Pointer<RangeCombinatoricContext> computeContextPointer =
                    Pointer.allocate(RangeCombinatoricContext.class).order(byteOrder);
                computeContextPointer.set(computeContext);

                CLBuffer<Integer> pageBuffer =
                    context.createBuffer(Usage.Output, Integer.class, batchSize * k);
                CLBuffer<RangeCombinatoricContext> contextBuffer =
                    context.createBuffer(Usage.Input, computeContextPointer);

                // load the program.
                CLKernel kernel = expandRange.createKernel("expand_comb_nr_range", contextBuffer, pageBuffer);

                CLEvent expandEvent = kernel.enqueueNDRange(queue, new int[] { batchSize });

                Pointer<Integer> elements = pageBuffer.read(queue, expandEvent);

                elementSetter.accept(e, elements);

                expandEvent.release();
                kernel.release();
                pageBuffer.release();
                contextBuffer.release();
                computeContextPointer.release();
              };
            }))

    // then expand the combination.
        .then(handler);

    disruptor.start();

    // feed ranges into the ring buffer.
    Ranges.rangeStream(Range.closedOpen(0L, CombinatoricsUtils.binomialCoefficient(elements.length, k)),
        batchSize).forEach(r -> {
      disruptor.publishEvent((e, s) -> {
        rangeSetter.accept(e, r);
      });
    });

    // Wait for the disruptor and halt.
    RingBuffer<E> buffer = disruptor.getRingBuffer();
    while( buffer.getBufferSize() != buffer.remainingCapacity() ) {
    	Thread.sleep(50);
    }
    disruptor.halt();
    
    // shutdown all the things.
    disruptor.shutdown();
    executor.shutdown();
  }

  private String expandRangeProgram() {
  	try {
      return IOUtils.readText(this.getClass().getResource("combinatoric.cl"));
  	}
  	catch(IOException ioe ) {
  		throw new RuntimeException(ioe);
  	}
  }
}