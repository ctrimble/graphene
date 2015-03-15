package com.xiantrimble.graphene;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.stream.IntStream;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

import com.google.common.collect.Range;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.util.IOUtils;
import com.xiantrimble.graphene.Graphene.ComputationContext;
import com.xiantrimble.graphene.Graphene.GrapheneDeviceContext;
import com.xiantrimble.graphene.source.IncludeResolvers;
import com.xiantrimble.graphene.source.SourcesBuilder;
import com.xiantrimble.graphene.source.Uris;

/**
 * Constructs graphs based on the following process.
 * 
 * @author Christian Trimble
 *
 */
public class GraphConstructor<P extends StructObject, C extends EdgeConstructionContext<P, C>, E extends AbstractEdgeStruct<?>> {
	
	public static class EdgePage<C extends StructObject, E extends AbstractEdgeStruct<?>> {
	  public EdgePage(Pointer<E> data, int length) {
	    this.data = data;
	    this.length = length;
    }
		public Pointer<E> getData() {
			return data;
		}
		public void setData(Pointer<E> data) {
			this.data = data;
		}
		public int getLength() {
			return length;
		}
		public void setLength(int length) {
			this.length = length;
		}
		Pointer<E> data;
	  int length;
	}
	
	public static class Builder<P extends StructObject, C extends EdgeConstructionContext<P, C>, E extends AbstractEdgeStruct<?>, B extends Builder<P, C, E, B>> {
		
		P partition;
		Graphene graphene;
		int pageSize = 10000;
		Class<E> edgeType;
		Supplier<C> newContext;
		Class<C> contextType;
		Function<P, String> sourceUri;
		
		@SuppressWarnings("unchecked")
    protected <T> T thisObject() {
			return (T)this;
		}
		
		public B withPartition( P partition ) {
			this.partition = partition;
			return thisObject();
		}
		
		public B withSourceUri( Function<P, String> sourceUri) {
			this.sourceUri = sourceUri;
			return thisObject();
		}

		public B withEdgeType( Class<E> edgeType) {
	    this.edgeType = edgeType;
	    return thisObject();
    }
		
		public B withGraphene( Graphene graphene ) {
			this.graphene = graphene;
			return thisObject();
		}
		
	  public B withPageSize( int pageSize ) {
	  	this.pageSize = pageSize;
	  	return thisObject();
	  }
	  
	  public B withContextType( Class<C> contextType ) {
	  	this.contextType = contextType;
	  	return thisObject();
	  }
	  
	  public B withContextSupplier( Supplier<C> newContext ) {
	  	this.newContext = newContext;
	  	return thisObject();
	  }
		
	  public GraphConstructor<P, C, E> build() {
	  	return new GraphConstructor<P, C, E>(partition, sourceUri, contextType, newContext, edgeType, pageSize, graphene);
	  }
	}

	public static class DefaultBuilder<P extends StructObject, C extends EdgeConstructionContext<P, C>, E extends AbstractEdgeStruct<?>> extends Builder<P, C, E, DefaultBuilder<P, C, E>>{};

	public static <P extends StructObject, C extends EdgeConstructionContext<P, C>, E extends AbstractEdgeStruct<?>> DefaultBuilder<P, C, E> builder() {
		return new DefaultBuilder<P, C, E>();
	}
	
	Class<E> edgeType;
	Class<C> contextType;
	Graphene graphene;
	P partition;
	int pageSize;
	Supplier<C> newContext;
	Function<P, String> sourceUri;
	
	public GraphConstructor(P partition, Function<P, String> sourceUri, Class<C> contextType, Supplier<C> newContext, Class<E> edgeType, int pageSize, Graphene graphene) {
		this.partition = partition;
		this.sourceUri = sourceUri;
		this.contextType = contextType;
		this.newContext = newContext;
		this.edgeType = edgeType;
		this.pageSize = pageSize;
		this.graphene = graphene;
  }


	@SuppressWarnings("unchecked")
  public void stream(IndexRange range, Consumer<EdgePage<C, E>> consumer) {
		// need a computation context here, coming from graphene.
		try( ComputationContext computationContext = graphene.createComputationContext() ) {
			Disruptor<Event<E>> disruptor = computationContext.createDisruptor(Event::new, 128);
			
			disruptor.handleExceptionsWith(new ExceptionHandler() {

				@Override
        public void handleEventException(Throwable ex, long sequence,
            Object event) {
	        ex.printStackTrace();
	        
        }

				@Override
        public void handleOnStartException(Throwable ex) {
	        ex.printStackTrace();
        }

				@Override
        public void handleOnShutdownException(Throwable ex) {
	        ex.printStackTrace();
        }
				
			});
			
			// for each device, create a set of workers.	
			EventHandlerGroup<Event<E>> poolGroup = disruptor.handleEventsWithWorkerPool(computationContext.getDeviceComputationContexts().stream().flatMap(d->{
				// do things here that are done once per device, like load programs.
				CLProgram program = d.createProgram(sources(partition, sourceUri));
				
				return IntStream.range(0, 10).<WorkHandler<Event<E>>>mapToObj(i->{
					C context = newContext.get()
					  .edgesLength(pageSize)
					  .range(range)
					  .partition(partition)
					  .overflowIndex(-1);
					
					// create pointers.
					Pointer<C> contextPointer = Pointer.allocate(contextType).order(d.getDeviceContext().getByteOrder());
					contextPointer.set(context);
					Pointer<E> edgePointer = Pointer.allocateArray(edgeType, pageSize);

					// create buffers.
					CLBuffer<C> contextBuffer = d.createBuffer(Usage.Input, contextPointer);
					CLBuffer<E> edgeBuffer = d.createBuffer(Usage.Output, edgePointer);

					// create the kernel
					CLKernel kernel = program.createKernel("constructEdges", contextBuffer, edgeBuffer);
					
					return new WorkHandlerWithLifecycle<Event<E>>() {
						@Override
            public void onStart() {}
						
						@Override
            public void onEvent(Event<E> event) throws Exception {
							// reset the context
							
							CLEvent kernelEvent = kernel.enqueueNDRange(d.getDeviceContext().getQueue(), new int[] { pageSize });
							
							Pointer<E> edges = edgeBuffer.read(d.getDeviceContext().getQueue(), kernelEvent);
							
							kernelEvent.release();
	            
							// put data in event.
							event.edges = edges;
            }

						@Override
            public void onShutdown() {
							contextPointer.release();
							kernel.release();
						}
					};
				});
			}).toArray(WorkHandler[]::new));
			
			poolGroup.then(new EventHandler<Event<E>>() {

				@Override
        public void onEvent(Event<E> event, long sequence, boolean endOfBatch)
            throws Exception {
					EdgePage<C, E> page = new EdgePage<C, E>(event.edges, (int)(event.pageRange.end()-event.pageRange.start()));
	        consumer.accept(page);
        }
				
			});
			
			// start the disruptor.
			disruptor.start();
			
			// stream ranges into the disruptor.
			Ranges.rangeStream(range, pageSize)
			  .forEach(pageRange->{
			  	disruptor.publishEvent((event, sequence)->{
			  		event.pageRange = pageRange;
			  	});
			  });
		}
	}
	
	private String[] sources(P partition, Function<P, String> sourceUri) {
		return SourcesBuilder.builder()
		  .withBaseUri(Uris.uriStringFor(partition.getClass()))
		  .withIncludeResolver(IncludeResolvers.defaultResolver())
		  .build()
		  .addUri(sourceUri.apply(partition))
		  .build();
  }

	public static interface WorkHandlerWithLifecycle<T> extends WorkHandler<T>, LifecycleAware {}
	
	public static class Event<E extends AbstractEdgeStruct<?>> {
		public Pointer<E> edges;
		public IndexRange pageRange;
	}
}


/* high level C99 Code

  // this function is library provided.
  kernel constructEdges( Context* context, Edge* edges ) {
    long index = global_id(0);
    
    // test to see if we are in range.
    
    // test to see if we have generated too many edges.

    visitNextVertices( index, vertex, context, edges );
  }
  
  // this function is user provided.
  void visitNextVertices( long index, Vertex vertex, Context* context, Edge* edges ) {
  
    Vertex vertex = vertexFor( context->partition, index );
  
    // custom logic here to generate next vertices
    
    // each time a new edge is encountered...
    int nextVertexIndex = indexOf( context->nextPartition, nextVertex );
    
    generateEdge( index, nextVertexIndex, context, edges );
  }
  
  // this function is library provided.
  void generateEdge( long fromIndex, long toIndex, Context* context, Edge* edges ) {
    int edgeIndex = atomic_add(context->edgeCount);
    
    if( edgeIndex >= context->edgeLength ) {
      // we have filled our output buffer, crap!
      // use atomic_min to track lowest index that failed, so we can restart.
    }
    
    // set the edge at edgeIndex with this edge information.
  }
*/
