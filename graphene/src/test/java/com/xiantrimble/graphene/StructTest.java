package com.xiantrimble.graphene;

import static org.bridj.Pointer.allocateBytes;
import static org.bridj.Pointer.allocateInts;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteOrder;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.Matchers.*;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.util.IOUtils;
import com.xiantrimble.graphene.junit.OpenCLRequiredRule;

/**
 * A very simple graph scatter and gather implementation using structs.  This example has a single
 * data point for each vertex that holds a distance.  The computation then computes the relative distances
 * for the remaining nodes.
 * 
 * Take aways from building this example:
 * 
 * - Edge records should use to and from to specify the related nodes, instead of in and out.  Those
 * names were vertex centric as opposed to edge centric.
 * - Relative resources need to start with `./` when being loaded.
 * - OpenCL has the constants found in `limits.h` preloaded.  There is no need to include them.
 * - When implementing BridJ Structs by hand, static final fields should be used to bind the implementations
 * and annotations together.
 * 
 * @author Christian Trimble
 *
 */
public class StructTest {
	
	public static @ClassRule OpenCLRequiredRule openClAvailable = new OpenCLRequiredRule();
	
	public final static long A = 0;
	public final static long B = 1;
	public final static long C = 2;	
	public final static long D = 3;
	public final static long E = 4;
	public final static long F = 5;	
	
	public final static int MAX_ROUNDS = 30;

  @Test
  public void shouldGatherData() throws IOException {

		// This should all be coming from the outside.
		int vertexLength = 6;
		int edgeLength = 7;
		
		CLContext context = JavaCL.createBestContext();
		CLQueue queue = context.createDefaultQueue();
		ByteOrder byteOrder = context.getByteOrder();

		Pointer<VertexStruct> vertexArrayPointer = Pointer.allocateArray(VertexStruct.class, vertexLength).order(byteOrder);
		Pointer<EdgeStruct> edgeArrayPointer = Pointer.allocateArray(EdgeStruct.class, edgeLength).order(byteOrder);
		Pointer<ContextStruct> contextPointer = Pointer.allocate(ContextStruct.class).order(byteOrder);
		
		// a test data set.
		int ei = 0;
		vertexArrayPointer.get(A).data(1);
		edgeArrayPointer.get(ei++).from(B).to(A);
		edgeArrayPointer.get(ei++).from(D).to(A);
		
		vertexArrayPointer.get(B).data(Long.MAX_VALUE);
		edgeArrayPointer.get(ei++).from(D).to(B);
		
		vertexArrayPointer.get(C).data(Long.MAX_VALUE);
		
		vertexArrayPointer.get(D).data(Long.MAX_VALUE);		
		edgeArrayPointer.get(ei++).from(C).to(D);
		
		vertexArrayPointer.get(E).data(Long.MAX_VALUE);		
		edgeArrayPointer.get(ei++).from(C).to(E);
		edgeArrayPointer.get(ei++).from(D).to(E);

		vertexArrayPointer.get(F).data(1);
		edgeArrayPointer.get(ei++).from(E).to(F);
		
		contextPointer.get().edgeLength(edgeLength);
		contextPointer.get().vertexLength(vertexLength);
		contextPointer.get().updates(0);
		
		// start OpenCL specific code.
		
		CLBuffer<VertexStruct> vertexBuffer = context.createBuffer(Usage.InputOutput, vertexArrayPointer);
		CLBuffer<EdgeStruct> edgeBuffer = context.createBuffer(Usage.InputOutput, edgeArrayPointer);
		CLBuffer<ContextStruct> contextBuffer = context.createBuffer(Usage.InputOutput, contextPointer);
		
		URL url = StructTest.class.getResource("./test.cl");
    String src = IOUtils.readText(url);
    CLProgram program = context.createProgram(src);
    
    CLKernel gatherKernel = program.createKernel("gather");
    CLKernel scatterKernel = program.createKernel("scatter");
    gatherKernel.setArgs(contextBuffer, vertexBuffer, edgeBuffer);
    scatterKernel.setArgs(contextBuffer, vertexBuffer, edgeBuffer);
    
    // scatter across vertex page and edge page Cartesian product.  Could reduce this by several thousand
    // times if I grouped things by king movement.  In other words, if I could easily judge the distance
    // between groups of edges, then loading could be reduced.
    CLEvent scatterEvent = scatterKernel.enqueueNDRange(queue, new int[] { edgeLength });
    
    // gather vertex page and edge page pairs.
    // if the edges are sorted by in and do not cross boundaries, then update counts will work.
    // also, the number of read/write operations would be reduced significantly.
    CLEvent gatherEvent = gatherKernel.enqueueNDRange(queue, new int[] { vertexLength }, scatterEvent);

    ContextStruct kernelContext;
    for(int i = 0; i < MAX_ROUNDS && (kernelContext = contextBuffer.read(queue, gatherEvent).get()).updates() > 0; i++ ) {
    	System.out.println("Round "+i);
    	kernelContext.updates(0);
    	CLEvent contextUpdate = contextBuffer.write(queue, contextPointer, false);
    	scatterEvent = scatterKernel.enqueueNDRange(queue, new int[] { edgeLength }, contextUpdate);
      gatherEvent = gatherKernel.enqueueNDRange(queue, new int[] { vertexLength }, scatterEvent);
    }
    
    vertexArrayPointer = vertexBuffer.read(queue);
    
    assertThat("A distiance updated", vertexArrayPointer.get(A).data(), equalTo(1L));
    assertThat("B distiance updated", vertexArrayPointer.get(B).data(), equalTo(2L));
    assertThat("C distiance updated", vertexArrayPointer.get(C).data(), equalTo(3L));
    assertThat("D distiance updated", vertexArrayPointer.get(D).data(), equalTo(2L));
    assertThat("E distiance updated", vertexArrayPointer.get(E).data(), equalTo(2L));
    assertThat("F distiance updated", vertexArrayPointer.get(F).data(), equalTo(1L));
  }
	
	public static Pointer<Byte> allocateAndLoad( byte[] value, ByteOrder byteOrder ) {
    Pointer<Byte> pointer = allocateBytes(value.length).order(byteOrder);
    pointer.setBytes(value);
    return pointer;		
	}
	
	public static Pointer<Integer> allocateAndLoad( int[] value, ByteOrder byteOrder ) {
    Pointer<Integer> pointer = allocateInts(value.length).order(byteOrder);
    pointer.setInts(value);
    return pointer;		
	}
	
	public static class ContextStruct extends StructObject {
		public static final int VERTEX_LENGTH_INDEX = 0;
		public static final int EDGE_LENGTH_INDEX = 1;
		public static final int UPDATES_INDEX = 2;
		public ContextStruct() {}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public ContextStruct( Pointer pointer ) {
			super(pointer);
		}
		
		@Field(VERTEX_LENGTH_INDEX)
		public long vertexLength() {
			return this.io.getLongField(this, VERTEX_LENGTH_INDEX);
		};
		
		@Field(VERTEX_LENGTH_INDEX)
		public ContextStruct vertexLength(long vertexLength) {
			this.io.setLongField(this, VERTEX_LENGTH_INDEX, vertexLength);
			return this;
		};
		
		@Field(EDGE_LENGTH_INDEX)
		public long edgeLength() {
			return this.io.getLongField(this, EDGE_LENGTH_INDEX);
		};
		
		@Field(EDGE_LENGTH_INDEX)
		public ContextStruct edgeLength(long edgeLength) {
			this.io.setLongField(this, EDGE_LENGTH_INDEX, edgeLength);
			return this;
		};

		@Field(UPDATES_INDEX)
		public int updates() {
			return this.io.getIntField(this, UPDATES_INDEX);
		};
		
		@Field(UPDATES_INDEX)
		public ContextStruct updates(int updates) {
			this.io.setIntField(this, UPDATES_INDEX, updates);
			return this;
		};
		
	}

	public static class VertexStruct extends StructObject {
		public static final int DATA_INDEX = 0;
		public VertexStruct() {}
		@SuppressWarnings({ "unchecked", "rawtypes" })
    public VertexStruct( Pointer pointer ) {
			super(pointer);
		}
		
		@Field(DATA_INDEX)
		public long data() {
			return this.io.getLongField(this, DATA_INDEX);
		};
		
		@Field(DATA_INDEX)
		public VertexStruct data(long data) {
			this.io.setLongField(this, DATA_INDEX, data);
			return this;
		};
		
	}
	
	public static class EdgeStruct extends StructObject {
		public static final int TO_INDEX = 0;
		public static final int FROM_INDEX = 1;
		public static final int DATA_INDEX = 2;
		public EdgeStruct(){}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public EdgeStruct( Pointer pointer ) {
			super(pointer);
		}
		
		@Field(TO_INDEX)
		public long to() {
			return this.io.getLongField(this, TO_INDEX);
		};
		
		@Field(TO_INDEX)
		public EdgeStruct to(long in) {
			this.io.setLongField(this, TO_INDEX, in);
			return this;
		};
		
		@Field(FROM_INDEX)
		public long from() {
			return this.io.getLongField(this, FROM_INDEX);
		};
		
		@Field(FROM_INDEX)
		public EdgeStruct from(long out) {
			this.io.setLongField(this, FROM_INDEX, out);
			return this;
		};

		@Field(DATA_INDEX)
		public long data() {
			return this.io.getLongField(this, DATA_INDEX);
		};
		
		@Field(DATA_INDEX)
		public EdgeStruct data(long data) {
			this.io.setLongField(this, DATA_INDEX, data);
			return this;
		};
		
	}
}
