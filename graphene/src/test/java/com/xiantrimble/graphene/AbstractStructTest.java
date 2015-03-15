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
package com.xiantrimble.graphene;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import com.xiantrimble.graphene.Graphene.GrapheneDeviceContext;
import com.xiantrimble.graphene.junit.GrapheneRule;
import com.xiantrimble.graphene.junit.GrapheneRules;
import com.xiantrimble.graphene.junit.OpenCLRequiredRule;

/**
 * A simple graph scatter and gather implementation using structs and typedefs.
 * 
 * ##Lessons Learned
 * 
 * ### Passing structs by value
 * 
 * I had some trouble passing structures by value.  When passing a nested structure from
 * global memory, I had to first set it to a variable.  So, code like this:
 * 
 * ```
 *   some_function(globalStruct.nested)
 * ```
 * 
 * needed to look like:
 * 
 * ```
 *   MyNestedStruct nested = globalStruct.nested;
 *   some_function(nested)
 * ```
 * 
 * Trying to resolve the issue be passing a pointer caused problems with
 * OpenCL's memory qualifiers.
 * 
 * ### JavaCL Structures and Generics
 * 
 * JavaCL's structure annotation's have a todo when searching for the
 * concrete type of a generic field.  This implementation would be grealty
 * simplified if this was resolved.
 * 
 * @author Christian Trimble
 *
 */
public class AbstractStructTest {

  public static @ClassRule GrapheneRule grapheneSupplier = GrapheneRules.graphene();

  public static class DataContext extends StructObject {
    public static final int VERTEX_OFFSET_INDEX = 0;
    public static final int VERTEX_LENGTH_INDEX = 1;
    public static final int EDGE_LENGTH_INDEX = 2;

    public DataContext() {
    }

    public DataContext(Pointer<? extends StructObject> pointer) {
      super(pointer);
    }

    @Field(VERTEX_OFFSET_INDEX)
    public long vertexOffset() {
      return this.io.getLongField(this, VERTEX_OFFSET_INDEX);
    };

    @Field(VERTEX_OFFSET_INDEX)
    public DataContext vertexOffset(long vertexOffset) {
      this.io.setLongField(this, VERTEX_OFFSET_INDEX, vertexOffset);
      return this;
    };

    @Field(VERTEX_LENGTH_INDEX)
    public int vertexLength() {
      return this.io.getIntField(this, VERTEX_LENGTH_INDEX);
    };

    @Field(VERTEX_LENGTH_INDEX)
    public DataContext vertexLength(int vertexLength) {
      this.io.setIntField(this, VERTEX_LENGTH_INDEX, vertexLength);
      return this;
    };

    @Field(EDGE_LENGTH_INDEX)
    public int edgeLength() {
      return this.io.getIntField(this, EDGE_LENGTH_INDEX);
    };

    @Field(EDGE_LENGTH_INDEX)
    public DataContext edgeLength(int edgeLength) {
      this.io.setIntField(this, EDGE_LENGTH_INDEX, edgeLength);
      return this;
    };
  }

  public static class ComputeContext extends StructObject {
    public static final int UPDATES_INDEX = 0;

    public ComputeContext() {
    }

    public ComputeContext(Pointer<? extends StructObject> pointer) {
      super(pointer);
    }

    @Field(UPDATES_INDEX)
    public int updates() {
      return this.io.getIntField(this, UPDATES_INDEX);
    };

    @Field(UPDATES_INDEX)
    public ComputeContext updates(int updates) {
      this.io.setIntField(this, UPDATES_INDEX, updates);
      return this;
    };
  }

  /**
   * A basic graph solver that demonstrates the concerns that must be
   * dealt with when using OpenCL from Java when generics are involved.
   * 
   * @author Christian Trimble
   *
   * @param <D> The Java type of the vertex value struct.
   */
  public static class GraphSolver<D extends StructObject> {

    public final static int MAX_ROUNDS = 30;
    //
    // These need to be supplied by the program defining and
    // running a specific problem.
    //
    Supplier<URL> resourceSupplier;
    Consumer<GraphSolver<D>> assertResult;
    Consumer<GraphSolver<D>> buildProblem;
    Class<? extends AbstractVertexStruct<D>> vertexType;
    Class<? extends AbstractEdgeStruct<D>> edgeType;

    //
    // These must be allocated and defined while building the problem.
    //
    Pointer<? extends AbstractVertexStruct<D>> vertexArrayPointer;
    int vertexLength = -1; // this may be in the pointer.
    Pointer<? extends AbstractEdgeStruct<D>> edgeArrayPointer;
    int edgeLength = -1; // this may be in the pointer.

    //
    // Context information about the problem and state of the
    // computation. Managed by the solver.
    //
    Pointer<DataContext> contextPointer;
    Pointer<ComputeContext> computeContextPointer;

    //
    // Components of the OpenCL API.
    //
    GrapheneDeviceContext deviceContext = grapheneSupplier.get().getContexts().get(0);

    public GraphSolver<D> withResourceSupplier(Supplier<URL> resourceSupplier) {
      this.resourceSupplier = resourceSupplier;
      return this;
    }

    public GraphSolver<D> withAssertResult(Consumer<GraphSolver<D>> assertResult) {
      this.assertResult = assertResult;
      return this;
    }

    public GraphSolver<D> withBuildProblem(Consumer<GraphSolver<D>> buildProblem) {
      this.buildProblem = buildProblem;
      return this;
    }

    public GraphSolver<D> withVertexType(Class<? extends AbstractVertexStruct<D>> vertexType) {
      this.vertexType = vertexType;
      return this;
    }

    public GraphSolver<D> withEdgeType(Class<? extends AbstractEdgeStruct<D>> edgeType) {
      this.edgeType = edgeType;
      return this;
    }

    public Pointer<? extends AbstractVertexStruct<D>> allocateVertices(int vertexLength) {
      this.vertexLength = vertexLength;
      contextPointer.get().vertexLength(vertexLength);
      return vertexArrayPointer = Pointer.allocateArray(vertexType, vertexLength).order(deviceContext.getByteOrder());
    }

    public Pointer<? extends AbstractEdgeStruct<D>> allocateEdges(int edgeLength) {
      this.edgeLength = edgeLength;
      contextPointer.get().edgeLength(edgeLength);
      return edgeArrayPointer = Pointer.allocateArray(edgeType, edgeLength).order(deviceContext.getByteOrder());
    }

    public void doTest() throws IOException {

      contextPointer = Pointer.allocate(DataContext.class).order(deviceContext.getByteOrder());
      computeContextPointer = Pointer.allocate(ComputeContext.class).order(deviceContext.getByteOrder());

      contextPointer.get().vertexOffset(0);
      computeContextPointer.get().updates(0);
      buildProblem.accept(this);

      CLBuffer<? extends AbstractVertexStruct<D>> vertexBuffer =
          deviceContext.getContext().createBuffer(Usage.InputOutput, vertexArrayPointer);
      CLBuffer<? extends AbstractEdgeStruct<D>> edgeBuffer =
      		deviceContext.getContext().createBuffer(Usage.InputOutput, edgeArrayPointer);
      CLBuffer<DataContext> contextBuffer = deviceContext.getContext().createBuffer(Usage.InputOutput, contextPointer);
      CLBuffer<ComputeContext> computeContextBuffer =
      		deviceContext.getContext().createBuffer(Usage.InputOutput, computeContextPointer);

      CLProgram program;
      if (resourceSupplier != null) {
        program =
        		deviceContext.getContext().createProgram(IOUtils.readText(resourceSupplier.get()),
                IOUtils.readText(AbstractStructTest.class.getResource("./generic_data_test.cl")));
      } else {
        program =
        		deviceContext.getContext().createProgram(IOUtils.readText(AbstractStructTest.class
                .getResource("./generic_data_test.cl")));
      }

      CLKernel gatherKernel = program.createKernel("gather");
      CLKernel scatterKernel = program.createKernel("scatter");
      gatherKernel.setArgs(computeContextBuffer, contextBuffer, vertexBuffer, edgeBuffer);
      scatterKernel.setArgs(computeContextBuffer, contextBuffer, vertexBuffer, edgeBuffer);

      CLEvent scatterEvent = scatterKernel.enqueueNDRange(deviceContext.getQueue(), new int[] { edgeLength });

      CLEvent gatherEvent =
          gatherKernel.enqueueNDRange(deviceContext.getQueue(), new int[] { vertexLength }, scatterEvent);

      ComputeContext kernelContext;
      for (int i = 0; i < MAX_ROUNDS
          && (kernelContext = computeContextBuffer.read(deviceContext.getQueue(), gatherEvent).get()).updates() > 0; i++) {
        System.out.println("Round " + i);
        // release previous round resources
        scatterEvent.release();
        gatherEvent.release();

        kernelContext.updates(0);
        CLEvent contextUpdate = computeContextBuffer.write(deviceContext.getQueue(), computeContextPointer, false);
        scatterEvent = scatterKernel.enqueueNDRange(deviceContext.getQueue(), new int[] { edgeLength }, contextUpdate);
        gatherEvent = gatherKernel.enqueueNDRange(deviceContext.getQueue(), new int[] { vertexLength }, scatterEvent);

        // release resources.
        contextUpdate.release();
      }

      scatterEvent.release();
      gatherEvent.release();

      vertexArrayPointer = vertexBuffer.read(deviceContext.getQueue());

      assertResult.accept(this);

      // release the kernels.
      gatherKernel.release();
      scatterKernel.release();

      // release the program
      program.release();

      // release the buffers.
      vertexBuffer.release();
      edgeBuffer.release();
      contextBuffer.release();
      computeContextBuffer.release();

      // release the pointers.
      vertexArrayPointer.release();
      edgeArrayPointer.release();
      contextPointer.release();
      computeContextPointer.release();
    }
  }

  //
  // Common constants and methods used in test graph building.
  //

  public final static long A = 0;
  public final static long B = 1;
  public final static long C = 2;
  public final static long D = 3;
  public final static long E = 4;
  public final static long F = 5;
  public final static long G = 6;
  public final static long H = 7;
  public final static long I = 8;
  public final static long J = 9;

  public static <T extends StructObject> void initVertex(AbstractVertexStruct<T> vertexStruct, T value,
      T initialValue) {
    vertexStruct.value(value).initialValue(initialValue);
  }

  //
  // Test for the default vertex value struct.
  //
  public static class DefaultValue extends StructObject {
    public static final int DISTANCE_INDEX = 0;

    public DefaultValue() {
    }

    public DefaultValue(Pointer<? extends StructObject> pointer) {
      super(pointer);
    }

    @Field(DISTANCE_INDEX)
    public int distance() {
      return this.io.getIntField(this, DISTANCE_INDEX);
    }

    @Field(DISTANCE_INDEX)
    public DefaultValue distance(int distance) {
      this.io.setIntField(this, DISTANCE_INDEX, distance);
      return this;
    }
  }

  public static class DefaultVertex extends AbstractVertexStruct<DefaultValue> {

    @Field(VALUE_INDEX)
    public DefaultValue value() {
      return this.io.getNativeObjectField(this, VALUE_INDEX);
    };

    @Field(VALUE_INDEX)
    public AbstractVertexStruct<DefaultValue> value(DefaultValue data) {
      this.io.setNativeObjectField(this, VALUE_INDEX, data);
      return this;
    };

    @Field(INITIAL_VALUE_INDEX)
    public DefaultValue initialValue() {
      return this.io.getNativeObjectField(this, INITIAL_VALUE_INDEX);
    };

    @Field(INITIAL_VALUE_INDEX)
    public AbstractVertexStruct<DefaultValue> initialValue(DefaultValue initialValue) {
      this.io.setNativeObjectField(this, INITIAL_VALUE_INDEX, initialValue);
      return this;
    };
  }

  public static class DefaultEdge extends AbstractEdgeStruct<DefaultValue> {
    @Field(TO_VALUE_INDEX)
    public DefaultValue toValue() {
      return this.io.getNativeObjectField(this, TO_VALUE_INDEX);
    };

    @Field(TO_VALUE_INDEX)
    public AbstractEdgeStruct<DefaultValue> toValue(DefaultValue toValue) {
      this.io.setNativeObjectField(this, TO_VALUE_INDEX, toValue);
      return this;
    };
  }

  public static DefaultValue defaultValue(int distance) {
    return new DefaultValue().distance(distance);
  }

  @Test
  public void shouldSolveWithDefaultType() throws IOException {
    new GraphSolver<DefaultValue>()
        .withVertexType(DefaultVertex.class)
        .withEdgeType(DefaultEdge.class)
        .withBuildProblem(gs -> {
          Pointer<? extends AbstractVertexStruct<DefaultValue>> vertexArray = gs.allocateVertices(6);
          Pointer<? extends AbstractEdgeStruct<DefaultValue>> edgeArray = gs.allocateEdges(7);

          // test vertices
            initVertex(vertexArray.get(A), defaultValue(1), defaultValue(1));
            initVertex(vertexArray.get(B), defaultValue(Integer.MAX_VALUE),
                defaultValue(Integer.MAX_VALUE));
            initVertex(vertexArray.get(C), defaultValue(Integer.MAX_VALUE),
                defaultValue(Integer.MAX_VALUE));
            initVertex(vertexArray.get(D), defaultValue(Integer.MAX_VALUE),
                defaultValue(Integer.MAX_VALUE));
            initVertex(vertexArray.get(E), defaultValue(Integer.MAX_VALUE),
                defaultValue(Integer.MAX_VALUE));
            initVertex(vertexArray.get(F), defaultValue(1), defaultValue(1));
            ;

            // test edges
            int ei = 0;
            edgeArray.get(ei++).from(B).to(A);
            edgeArray.get(ei++).from(D).to(A);
            edgeArray.get(ei++).from(D).to(B);
            edgeArray.get(ei++).from(C).to(D);
            edgeArray.get(ei++).from(C).to(E);
            edgeArray.get(ei++).from(D).to(E);
            edgeArray.get(ei++).from(E).to(F);
          })
        .withAssertResult(
            gs -> {
              assertThat("A distiance updated", gs.vertexArrayPointer.get(A).value(),
                  equalTo(defaultValue(1)));
              assertThat("B distiance updated", gs.vertexArrayPointer.get(B).value(),
                  equalTo(defaultValue(2)));
              assertThat("C distiance updated", gs.vertexArrayPointer.get(C).value(),
                  equalTo(defaultValue(3)));
              assertThat("D distiance updated", gs.vertexArrayPointer.get(D).value(),
                  equalTo(defaultValue(2)));
              assertThat("E distiance updated", gs.vertexArrayPointer.get(E).value(),
                  equalTo(defaultValue(2)));
              assertThat("F distiance updated", gs.vertexArrayPointer.get(F).value(),
                  equalTo(defaultValue(1)));
            }).doTest();
  }

  //
  // Test for the custom vertex value struct.
  //
  public static final int WIN = 4;
  public static final int CYCLE = 3;
  public static final int STALEMATE = 2;
  public static final int LOSS = 1;
  public static final int UNDEFINED = 0;

  public static CustomValue customValue(int result, int depth) {
    return new CustomValue().result(result).depth(depth);
  }

  @Test
  public void shouldSolveWithCustomType() throws IOException {
    new GraphSolver<CustomValue>()
        .withResourceSupplier(() -> AbstractStructTest.class.getResource("./complex_value_test.cl"))
        .withVertexType(CustomVertex.class)
        .withEdgeType(CustomEdge.class)
        .withBuildProblem(gs -> {
          Pointer<? extends AbstractVertexStruct<CustomValue>> vertexArray = gs.allocateVertices(10);
          Pointer<? extends AbstractEdgeStruct<CustomValue>> edgeArray = gs.allocateEdges(11);

          // test vertices
            initVertex(vertexArray.get(A), customValue(LOSS, 0), customValue(LOSS, 0));
            initVertex(vertexArray.get(B), customValue(CYCLE, 0), customValue(UNDEFINED, 0));
            initVertex(vertexArray.get(C), customValue(CYCLE, 0), customValue(UNDEFINED, 0));
            initVertex(vertexArray.get(D), customValue(CYCLE, 0), customValue(UNDEFINED, 0));
            initVertex(vertexArray.get(E), customValue(CYCLE, 0), customValue(UNDEFINED, 0));
            initVertex(vertexArray.get(F), customValue(CYCLE, 0), customValue(UNDEFINED, 0));
            initVertex(vertexArray.get(G), customValue(CYCLE, 0), customValue(UNDEFINED, 0));
            initVertex(vertexArray.get(H), customValue(CYCLE, 0), customValue(UNDEFINED, 0));
            initVertex(vertexArray.get(I), customValue(CYCLE, 0), customValue(UNDEFINED, 0));
            initVertex(vertexArray.get(J), customValue(LOSS, 4), customValue(LOSS, 4));

            // test edges
            int ei = 0;
            edgeArray.get(ei++).to(A).from(B);
            edgeArray.get(ei++).to(B).from(D);
            edgeArray.get(ei++).to(C).from(B);
            edgeArray.get(ei++).to(C).from(G);
            edgeArray.get(ei++).to(D).from(H);
            edgeArray.get(ei++).to(E).from(C);
            edgeArray.get(ei++).to(E).from(I);
            edgeArray.get(ei++).to(F).from(E);
            edgeArray.get(ei++).to(G).from(F);
            edgeArray.get(ei++).to(H).from(I);
            edgeArray.get(ei++).to(J).from(H);
          })
        .withAssertResult(
            gs -> {
              assertThat("A distiance updated", gs.vertexArrayPointer.get(A).value(),
                  equalTo(customValue(LOSS, 0)));
              assertThat("B distiance updated", gs.vertexArrayPointer.get(B).value(),
                  equalTo(customValue(WIN, 1)));
              assertThat("C distiance updated", gs.vertexArrayPointer.get(C).value(),
                  equalTo(customValue(CYCLE, 0)));
              assertThat("D distiance updated", gs.vertexArrayPointer.get(D).value(),
                  equalTo(customValue(LOSS, 2)));
              assertThat("E distiance updated", gs.vertexArrayPointer.get(E).value(),
                  equalTo(customValue(CYCLE, 0)));
              assertThat("F distiance updated", gs.vertexArrayPointer.get(F).value(),
                  equalTo(customValue(CYCLE, 0)));
              assertThat("G distiance updated", gs.vertexArrayPointer.get(G).value(),
                  equalTo(customValue(CYCLE, 0)));
              assertThat("H distiance updated", gs.vertexArrayPointer.get(H).value(),
                  equalTo(customValue(WIN, 3)));
              assertThat("I distiance updated", gs.vertexArrayPointer.get(I).value(),
                  equalTo(customValue(CYCLE, 0)));
              assertThat("J distiance updated", gs.vertexArrayPointer.get(J).value(),
                  equalTo(customValue(LOSS, 4)));
            }).doTest();
  }

}
