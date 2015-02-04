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
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.junit.ClassRule;
import org.junit.Test;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.util.IOUtils;
import com.xiantrimble.graphene.AbstractStructTest.ComputeContext;
import com.xiantrimble.graphene.AbstractStructTest.DataContext;
import com.xiantrimble.graphene.AbstractStructTest.EdgeStruct;
import com.xiantrimble.graphene.AbstractStructTest.GraphSolver;
import com.xiantrimble.graphene.AbstractStructTest.VertexStruct;
import com.xiantrimble.graphene.junit.OpenCLRequiredRule;

public class CombinatoricTest {

  public static @ClassRule OpenCLRequiredRule openClAvailable = new OpenCLRequiredRule();

  public static class BinomialCoefficientContext extends StructObject {
    public static final int MAX_UNIQUE_ELEMENTS = 256;
    public static final int N_OFFSET_OFFSET = 0;
    public static final int K_OFFSET_OFFSET = 1;

    public BinomialCoefficientContext() {
    }

    public BinomialCoefficientContext(Pointer<? extends StructObject> pointer) {
      super(pointer);
    }

    @Field(N_OFFSET_OFFSET)
    public int nOffset() {
      return this.io.getIntField(this, N_OFFSET_OFFSET);
    };

    @Field(N_OFFSET_OFFSET)
    public BinomialCoefficientContext nOffset(int nOffset) {
      this.io.setIntField(this, N_OFFSET_OFFSET, nOffset);
      return this;
    };

    @Field(K_OFFSET_OFFSET)
    public int kOffset() {
      return this.io.getIntField(this, K_OFFSET_OFFSET);
    };

    @Field(K_OFFSET_OFFSET)
    public BinomialCoefficientContext kOffset(int kOffset) {
      this.io.setIntField(this, K_OFFSET_OFFSET, kOffset);
      return this;
    };
  }

  public static class BinomialCoefficientTable {
    //
    // These need to be supplied by the program defining and
    // running a specific problem.
    //
    Consumer<BinomialCoefficientTable> assertResult;

    //
    // These must be allocated and defined while building the problem.
    //
    int nOffset = 0;
    int kOffset = 0;
    int nLength = -1;
    int kLength = -1;
    Pointer<Long> resultPointer;

    //
    // Context information about the problem and state of the
    // computation. Managed by the solver.
    //
    Pointer<BinomialCoefficientContext> contextPointer;

    //
    // Components of the OpenCL API.
    //
    CLContext context;
    CLQueue queue;
    ByteOrder byteOrder;

    public BinomialCoefficientTable withNOffset(int nOffset) {
      this.nOffset = nOffset;
      return this;
    }

    public BinomialCoefficientTable withNLength(int nLength) {
      this.nLength = nLength;
      return this;
    }

    public BinomialCoefficientTable withKOffset(int kOffset) {
      this.kOffset = kOffset;
      return this;
    }

    public BinomialCoefficientTable withKLength(int kLength) {
      this.kLength = kLength;
      return this;
    }

    public BinomialCoefficientTable withAssertResult(Consumer<BinomialCoefficientTable> assertResult) {
      this.assertResult = assertResult;
      return this;
    }

    public void doTest() throws IOException {

      context = JavaCL.createBestContext();
      queue = context.createDefaultQueue();
      byteOrder = context.getByteOrder();

      contextPointer = Pointer.allocate(BinomialCoefficientContext.class).order(byteOrder);
      resultPointer = Pointer.allocateLongs(nLength * kLength).order(byteOrder);

      contextPointer.set(new BinomialCoefficientContext().nOffset(nOffset).kOffset(kOffset));

      CLBuffer<Long> resultBuffer = context.createBuffer(Usage.InputOutput, resultPointer);
      CLBuffer<BinomialCoefficientContext> contextBuffer =
          context.createBuffer(Usage.InputOutput, contextPointer);

      CLProgram program =
          context.createProgram(IOUtils.readText(AbstractStructTest.class
              .getResource("./combinatoric.cl")));

      CLKernel expandKernel = program.createKernel("binomial_coefficient_table");
      expandKernel.setArgs(contextBuffer, resultBuffer);

      CLEvent expandEvent = expandKernel.enqueueNDRange(queue, new int[] { nLength, kLength });

      resultPointer = resultBuffer.read(queue, expandEvent);

      assertResult.accept(this);

      context.release();
    }

    public Long getResult(int n, int k) {
      int index = ((n - nOffset) * kLength) + (k - kOffset);
      return resultPointer.get(index);
    }
  }

  @Test
  public void shouldCreateBinaryCoefficients() throws IOException {
    new BinomialCoefficientTable().withNLength(20).withKLength(20).withAssertResult(t -> {
      assertChoose(t, 0, 0, 1L);
      assertChoose(t, 1, 1, 1L);
      assertChoose(t, 2, 2, 1L);
      assertChoose(t, 3, 3, 1L);
      assertChoose(t, 4, 0, 1L);
      assertChoose(t, 4, 1, 4L);
      assertChoose(t, 4, 2, 6L);
      assertChoose(t, 4, 3, 4L);
      assertChoose(t, 4, 4, 1L);
      assertChoose(t, 5, 0, 1L);
      assertChoose(t, 5, 1, 5L);
      assertChoose(t, 5, 2, 10L);
      assertChoose(t, 5, 3, 10L);
      assertChoose(t, 5, 4, 5L);
      assertChoose(t, 5, 5, 1L);
      assertChoose(t, 6, 6, 1L);
      assertChoose(t, 7, 7, 1L);
      assertChoose(t, 8, 8, 1L);
      assertChoose(t, 9, 9, 1L);
      assertChoose(t, 19, 19, 1L);
    }).doTest();
  }

  public static void assertChoose(BinomialCoefficientTable t, int n, int k, long r) {
    assertThat(n + " choose " + k, t.getResult(n, k), equalTo(r));
  }

  public static class CombinatoricContext extends StructObject {
    public static final int MAX_UNIQUE_ELEMENTS = 256;
    public static final int SIZE_OFFSET = 0;
    public static final int UNIQUE_ELEMENTS_OFFSET = 1;
    public static final int SUBSET_SIZE_OFFSET = 2;

    public CombinatoricContext() {
    }

    public CombinatoricContext(Pointer<? extends StructObject> pointer) {
      super(pointer);
    }

    @Field(SIZE_OFFSET)
    public int size() {
      return this.io.getIntField(this, SIZE_OFFSET);
    };

    @Field(SIZE_OFFSET)
    public CombinatoricContext size(int size) {
      this.io.setIntField(this, SIZE_OFFSET, size);
      return this;
    };

    @Field(SUBSET_SIZE_OFFSET)
    public int subsetSize() {
      return this.io.getIntField(this, SUBSET_SIZE_OFFSET);
    };

    @Field(SUBSET_SIZE_OFFSET)
    public CombinatoricContext subsetSize(int subsetSize) {
      this.io.setIntField(this, SUBSET_SIZE_OFFSET, subsetSize);
      return this;
    };

    @Field(UNIQUE_ELEMENTS_OFFSET)
    public int uniqueElements() {
      return this.io.getIntField(this, UNIQUE_ELEMENTS_OFFSET);
    };

    @Field(UNIQUE_ELEMENTS_OFFSET)
    public CombinatoricContext uniqueElements(int uniqueElements) {
      this.io.setIntField(this, UNIQUE_ELEMENTS_OFFSET, uniqueElements);
      return this;
    };
  }

  public static class CombinationTransition {

    @SuppressWarnings("unchecked")
    public CombinationTransition getThis() {
      return (CombinationTransition) this;
    }

    //
    // These need to be supplied by the program defining and
    // running a specific problem.
    //
    Supplier<URL> resourceSupplier;
    Consumer<CombinationTransition> assertResult;
    Consumer<CombinationTransition> buildProblem;

    //
    // These must be allocated and defined while building the problem.
    //
    public CombinatoricContext problemContext;
    String function;
    Pointer<Long> indicePointer;
    Pointer<Integer> elementPointer;

    //
    // Context information about the problem and state of the
    // computation. Managed by the solver.
    //
    Pointer<CombinatoricContext> contextPointer;

    //
    // Components of the OpenCL API.
    //
    CLContext context;
    CLQueue queue;
    ByteOrder byteOrder;

    public CombinationTransition withFunction(String function) {
      this.function = function;
      return getThis();
    }

    public CombinationTransition withProblemContext(CombinatoricContext problemContext) {
      this.problemContext = problemContext;
      return getThis();
    }

    public CombinationTransition withAssertResult(Consumer<CombinationTransition> assertResult) {
      this.assertResult = assertResult;
      return getThis();
    }

    public CombinationTransition withBuildProblem(Consumer<CombinationTransition> buildProblem) {
      this.buildProblem = buildProblem;
      return getThis();
    }

    public void doTest() throws IOException {

      context = JavaCL.createBestContext();
      queue = context.createDefaultQueue();
      byteOrder = context.getByteOrder();

      contextPointer = Pointer.allocate(CombinatoricContext.class).order(byteOrder);
      indicePointer = Pointer.allocateLongs(problemContext.size()).order(byteOrder);
      elementPointer =
          Pointer.allocateInts(problemContext.size() * problemContext.subsetSize())
              .order(byteOrder);

      contextPointer.set(problemContext);

      buildProblem.accept(getThis());

      CLBuffer<Long> indiceBuffer = context.createBuffer(Usage.InputOutput, indicePointer);
      CLBuffer<Integer> elementBuffer = context.createBuffer(Usage.InputOutput, elementPointer);
      CLBuffer<CombinatoricContext> contextBuffer =
          context.createBuffer(Usage.InputOutput, contextPointer);

      CLProgram program =
          context.createProgram(IOUtils.readText(AbstractStructTest.class
              .getResource("./combinatoric.cl")));

      CLKernel combKernel = program.createKernel(function);
      combKernel.setArgs(contextBuffer, indiceBuffer, elementBuffer);

      CLEvent executeEvent = combKernel.enqueueNDRange(queue, new int[] { problemContext.size() });

      elementPointer = elementBuffer.read(queue, executeEvent);
      indicePointer = indiceBuffer.read(queue, executeEvent);

      assertResult.accept(getThis());

      context.release();
    }

    public int[] subset(int index) {
      int[] subset = new int[problemContext.subsetSize()];
      for (int e = 0; e < problemContext.subsetSize(); e++) {
        subset[e] = elementPointer.get((index * problemContext.subsetSize()) + e);
      }
      return subset;
    }

    public CombinationTransition subset(int index, int... elements) {
      for (int e = 0; e < elements.length; e++) {
        elementPointer.set((index * problemContext.subsetSize()) + e, elements[e]);
      }
      return this;
    }

    public long index(int index) {
      return this.indicePointer.get(index);
    }
  }

  @Test
  public void shouldExpandCombinationsNoRepetition() throws IOException {
    // we should have a builder here, so that we can compute the
    // max number of entries.
    new CombinationTransition().withFunction("expand_comb_nr")
        .withProblemContext(new CombinatoricContext().uniqueElements(6).subsetSize(3).size(20))
        .withBuildProblem(ce -> {
          for (int i = 0; i < ce.problemContext.size(); i++) {
            ce.indicePointer.set((long) i, (long) i);
          }
        }).withAssertResult(ce -> {
          assertSubset(ce, 0, 0, 1, 2);
          assertSubset(ce, 1, 0, 1, 3);
          assertSubset(ce, 2, 0, 1, 4);
          assertSubset(ce, 3, 0, 1, 5);
          assertSubset(ce, 4, 0, 2, 3);
          assertSubset(ce, 5, 0, 2, 4);
          assertSubset(ce, 6, 0, 2, 5);
          assertSubset(ce, 7, 0, 3, 4);
          assertSubset(ce, 8, 0, 3, 5);
          assertSubset(ce, 9, 0, 4, 5);
          assertSubset(ce, 10, 1, 2, 3);
          assertSubset(ce, 11, 1, 2, 4);
          assertSubset(ce, 12, 1, 2, 5);
          assertSubset(ce, 13, 1, 3, 4);
          assertSubset(ce, 14, 1, 3, 5);
          assertSubset(ce, 15, 1, 4, 5);
          assertSubset(ce, 16, 2, 3, 4);
          assertSubset(ce, 17, 2, 3, 5);
          assertSubset(ce, 18, 2, 4, 5);
          assertSubset(ce, 19, 3, 4, 5);
        }).doTest();
  }

  @Test
  public void shouldIndexCombinationsNoRepetition() throws IOException {
    new CombinationTransition().withFunction("index_comb_nr")
        .withProblemContext(new CombinatoricContext().uniqueElements(6).subsetSize(3).size(20))
        .withBuildProblem(ce -> {
          ce.subset(0, 0, 1, 2);
          ce.subset(1, 0, 1, 3);
          ce.subset(2, 0, 1, 4);
          ce.subset(3, 0, 1, 5);
          ce.subset(4, 0, 2, 3);
          ce.subset(5, 0, 2, 4);
          ce.subset(6, 0, 2, 5);
          ce.subset(7, 0, 3, 4);
          ce.subset(8, 0, 3, 5);
          ce.subset(9, 0, 4, 5);
          ce.subset(10, 1, 2, 3);
          ce.subset(11, 1, 2, 4);
          ce.subset(12, 1, 2, 5);
          ce.subset(13, 1, 3, 4);
          ce.subset(14, 1, 3, 5);
          ce.subset(15, 1, 4, 5);
          ce.subset(16, 2, 3, 4);
          ce.subset(17, 2, 3, 5);
          ce.subset(18, 2, 4, 5);
          ce.subset(19, 3, 4, 5);
        }).withAssertResult(ce -> {
          for (int i = 0; i < ce.problemContext.size(); i++) {
            assertIndex(ce, i, i);
          }
        }).doTest();
  }

  public static void assertSubset(CombinationTransition ce, int index, int... expected) {
    int[] actual = ce.subset(index);
    assertThat(index + " is correct", actual, equalTo(expected));
  }

  public static void assertIndex(CombinationTransition ce, int index, long expected) {
    long actual = ce.index(index);
    assertThat(index + " is correct", actual, equalTo(expected));
  }

  public static class PermutationTransition {

    @SuppressWarnings("unchecked")
    public PermutationTransition getThis() {
      return (PermutationTransition) this;
    }

    //
    // These need to be supplied by the program defining and
    // running a specific problem.
    //
    Supplier<URL> resourceSupplier;
    Consumer<PermutationTransition> assertResult;
    Consumer<PermutationTransition> buildProblem;

    //
    // These must be allocated and defined while building the problem.
    //
    public CombinatoricContext problemContext;
    public int[] elementCounts;
    String function;
    Pointer<Long> indicePointer;
    Pointer<Integer> elementPointer;

    //
    // Context information about the problem and state of the
    // computation. Managed by the solver.
    //
    Pointer<CombinatoricContext> contextPointer;
    Pointer<Integer> elementsPointer;

    //
    // Components of the OpenCL API.
    //
    CLContext context;
    CLQueue queue;
    ByteOrder byteOrder;

    public PermutationTransition withFunction(String function) {
      this.function = function;
      return getThis();
    }

    public PermutationTransition withProblemContext(CombinatoricContext problemContext) {
      this.problemContext = problemContext;
      return getThis();
    }

    public PermutationTransition withElementCount(int... elementCounts) {
      this.elementCounts = elementCounts;
      return getThis();
    }

    public PermutationTransition withAssertResult(Consumer<PermutationTransition> assertResult) {
      this.assertResult = assertResult;
      return getThis();
    }

    public PermutationTransition withBuildProblem(Consumer<PermutationTransition> buildProblem) {
      this.buildProblem = buildProblem;
      return getThis();
    }

    public void doTest() throws IOException {

      context = JavaCL.createBestContext();
      queue = context.createDefaultQueue();
      byteOrder = context.getByteOrder();

      contextPointer = Pointer.allocate(CombinatoricContext.class).order(byteOrder);
      indicePointer = Pointer.allocateLongs(problemContext.size()).order(byteOrder);
      elementsPointer = Pointer.allocateInts(problemContext.subsetSize()).order(byteOrder);
      elementPointer =
          Pointer.allocateInts(problemContext.size() * problemContext.subsetSize())
              .order(byteOrder);

      contextPointer.set(problemContext);
      elementsPointer.setInts(elementCounts);

      buildProblem.accept(getThis());

      CLBuffer<Long> indiceBuffer = context.createBuffer(Usage.InputOutput, indicePointer);
      CLBuffer<Integer> elementBuffer = context.createBuffer(Usage.InputOutput, elementPointer);
      CLBuffer<CombinatoricContext> contextBuffer =
          context.createBuffer(Usage.InputOutput, contextPointer);
      CLBuffer<Integer> elementsBuffer = context.createBuffer(Usage.Input, elementsPointer);

      CLProgram program =
          context.createProgram(
              "#define PERM_UNIQUE_ELEMENTS " + this.problemContext.uniqueElements() + "\n",
              IOUtils.readText(AbstractStructTest.class.getResource("./multiset_permutations.cl")));

      CLKernel combKernel = program.createKernel(function);
      combKernel.setArgs(contextBuffer, elementsBuffer, indiceBuffer, elementBuffer);

      CLEvent executeEvent = combKernel.enqueueNDRange(queue, new int[] { problemContext.size() });

      elementPointer = elementBuffer.read(queue, executeEvent);
      indicePointer = indiceBuffer.read(queue, executeEvent);

      assertResult.accept(getThis());

      context.release();
    }

    public int[] subset(int index) {
      int[] subset = new int[problemContext.subsetSize()];
      for (int e = 0; e < problemContext.subsetSize(); e++) {
        subset[e] = elementPointer.get((index * problemContext.subsetSize()) + e);
      }
      return subset;
    }

    public PermutationTransition subset(int index, int... elements) {
      for (int e = 0; e < elements.length; e++) {
        elementPointer.set((index * problemContext.subsetSize()) + e, elements[e]);
      }
      return this;
    }

    public long index(int index) {
      return this.indicePointer.get(index);
    }

    public PermutationTransition index(int index, long value) {
      this.indicePointer.set(index, value);
      return this;
    }
  }

  @Test
  public void shouldExpandMultisetPermutations() throws IOException {
    new PermutationTransition()
        .withProblemContext(new CombinatoricContext().size(30).uniqueElements(3).subsetSize(5))
        .withFunction("expand_perm_lr").withElementCount(2, 1, 2).withBuildProblem(t -> {
          for (int i = 0; i < t.problemContext.size(); i++) {
            t.index(i, i);
          }
        }).withAssertResult(t -> {
          int index = 0;
          assertThat(t.subset(index++), equalTo(elements(0, 0, 1, 2, 2)));
          assertThat(t.subset(index++), equalTo(elements(0, 0, 2, 1, 2)));
          assertThat(t.subset(index++), equalTo(elements(0, 0, 2, 2, 1)));
          assertThat(t.subset(index++), equalTo(elements(0, 1, 0, 2, 2)));
          assertThat(t.subset(index++), equalTo(elements(0, 1, 2, 0, 2)));
          assertThat(t.subset(index++), equalTo(elements(0, 1, 2, 2, 0)));
          assertThat(t.subset(index++), equalTo(elements(0, 2, 0, 1, 2)));
          assertThat(t.subset(index++), equalTo(elements(0, 2, 0, 2, 1)));
          assertThat(t.subset(index++), equalTo(elements(0, 2, 1, 0, 2)));
          assertThat(t.subset(index++), equalTo(elements(0, 2, 1, 2, 0)));

          assertThat(t.subset(index++), equalTo(elements(0, 2, 2, 0, 1)));
          assertThat(t.subset(index++), equalTo(elements(0, 2, 2, 1, 0)));
          assertThat(t.subset(index++), equalTo(elements(1, 0, 0, 2, 2)));
          assertThat(t.subset(index++), equalTo(elements(1, 0, 2, 0, 2)));
          assertThat(t.subset(index++), equalTo(elements(1, 0, 2, 2, 0)));
          assertThat(t.subset(index++), equalTo(elements(1, 2, 0, 0, 2)));
          assertThat(t.subset(index++), equalTo(elements(1, 2, 0, 2, 0)));
          assertThat(t.subset(index++), equalTo(elements(1, 2, 2, 0, 0)));
          assertThat(t.subset(index++), equalTo(elements(2, 0, 0, 1, 2)));
          assertThat(t.subset(index++), equalTo(elements(2, 0, 0, 2, 1)));

          assertThat(t.subset(index++), equalTo(elements(2, 0, 1, 0, 2)));
          assertThat(t.subset(index++), equalTo(elements(2, 0, 1, 2, 0)));
          assertThat(t.subset(index++), equalTo(elements(2, 0, 2, 0, 1)));
          assertThat(t.subset(index++), equalTo(elements(2, 0, 2, 1, 0)));
          assertThat(t.subset(index++), equalTo(elements(2, 1, 0, 0, 2)));
          assertThat(t.subset(index++), equalTo(elements(2, 1, 0, 2, 0)));
          assertThat(t.subset(index++), equalTo(elements(2, 1, 2, 0, 0)));
          assertThat(t.subset(index++), equalTo(elements(2, 2, 0, 0, 1)));
          assertThat(t.subset(index++), equalTo(elements(2, 2, 0, 1, 0)));
          assertThat(t.subset(index++), equalTo(elements(2, 2, 1, 0, 0)));
        }).doTest();
  }

  @Test
  public void shouldIndexMultisetPermutations() throws IOException {
    new PermutationTransition()
        .withProblemContext(new CombinatoricContext().size(30).uniqueElements(3).subsetSize(5))
        .withFunction("index_perm_lr").withElementCount(2, 1, 2).withBuildProblem(t -> {
          int index = 0;
          t.subset(index++, 0, 0, 1, 2, 2);
          t.subset(index++, 0, 0, 2, 1, 2);
          t.subset(index++, 0, 0, 2, 2, 1);
          t.subset(index++, 0, 1, 0, 2, 2);
          t.subset(index++, 0, 1, 2, 0, 2);
          t.subset(index++, 0, 1, 2, 2, 0);
          t.subset(index++, 0, 2, 0, 1, 2);
          t.subset(index++, 0, 2, 0, 2, 1);
          t.subset(index++, 0, 2, 1, 0, 2);
          t.subset(index++, 0, 2, 1, 2, 0);

          t.subset(index++, 0, 2, 2, 0, 1);
          t.subset(index++, 0, 2, 2, 1, 0);
          t.subset(index++, 1, 0, 0, 2, 2);
          t.subset(index++, 1, 0, 2, 0, 2);
          t.subset(index++, 1, 0, 2, 2, 0);
          t.subset(index++, 1, 2, 0, 0, 2);
          t.subset(index++, 1, 2, 0, 2, 0);
          t.subset(index++, 1, 2, 2, 0, 0);
          t.subset(index++, 2, 0, 0, 1, 2);
          t.subset(index++, 2, 0, 0, 2, 1);

          t.subset(index++, 2, 0, 1, 0, 2);
          t.subset(index++, 2, 0, 1, 2, 0);
          t.subset(index++, 2, 0, 2, 0, 1);
          t.subset(index++, 2, 0, 2, 1, 0);
          t.subset(index++, 2, 1, 0, 0, 2);
          t.subset(index++, 2, 1, 0, 2, 0);
          t.subset(index++, 2, 1, 2, 0, 0);
          t.subset(index++, 2, 2, 0, 0, 1);
          t.subset(index++, 2, 2, 0, 1, 0);
          t.subset(index++, 2, 2, 1, 0, 0);

        }).withAssertResult(t -> {
          for (int i = 0; i < t.problemContext.size(); i++) {
            assertThat(t.index(i), equalTo((long) i));
          }
        }).doTest();
  }

  public static int[] elements(int... elements) {
    return elements;
  }

  public static class PermutationSizeContext extends StructObject {
    public static final int MAX_UNIQUE_ELEMENTS = 256;
    public static final int SIZE_OFFSET = 0;
    public static final int UNIQUE_ELEMENTS_OFFSET = 1;

    public PermutationSizeContext() {
    }

    public PermutationSizeContext(Pointer<? extends StructObject> pointer) {
      super(pointer);
    }

    @Field(SIZE_OFFSET)
    public int size() {
      return this.io.getIntField(this, SIZE_OFFSET);
    };

    @Field(SIZE_OFFSET)
    public PermutationSizeContext size(int size) {
      this.io.setIntField(this, SIZE_OFFSET, size);
      return this;
    };

    @Field(UNIQUE_ELEMENTS_OFFSET)
    public int uniqueElements() {
      return this.io.getIntField(this, UNIQUE_ELEMENTS_OFFSET);
    };

    @Field(UNIQUE_ELEMENTS_OFFSET)
    public PermutationSizeContext uniqueElements(int uniqueElements) {
      this.io.setIntField(this, UNIQUE_ELEMENTS_OFFSET, uniqueElements);
      return this;
    };
  }

  public static class PermutationSizeTable {
    //
    // These need to be supplied by the program defining and
    // running a specific problem.
    //
    Consumer<PermutationSizeTable> assertResult;
    Consumer<PermutationSizeTable> buildProblem;

    //
    // These must be allocated and defined while building the problem.
    //
    PermutationSizeContext problemContext;
    Pointer<Integer> domainPointer;
    Pointer<Long> resultPointer;

    //
    // Context information about the problem and state of the
    // computation. Managed by the solver.
    //
    Pointer<PermutationSizeContext> contextPointer;

    //
    // Components of the OpenCL API.
    //
    CLContext context;
    CLQueue queue;
    ByteOrder byteOrder;

    public PermutationSizeTable withContext(PermutationSizeContext problemContext) {
      this.problemContext = problemContext;
      return this;
    }

    public PermutationSizeTable withBuildProblem(Consumer<PermutationSizeTable> buildProblem) {
      this.buildProblem = buildProblem;
      return this;
    }

    public PermutationSizeTable withAssertResult(Consumer<PermutationSizeTable> assertResult) {
      this.assertResult = assertResult;
      return this;
    }

    public void doTest() throws IOException {

      context = JavaCL.createBestContext();
      queue = context.createDefaultQueue();
      byteOrder = context.getByteOrder();

      contextPointer = Pointer.allocate(PermutationSizeContext.class).order(byteOrder);
      domainPointer =
          Pointer.allocateInts(problemContext.size() * problemContext.uniqueElements()).order(
              byteOrder);
      resultPointer = Pointer.allocateLongs(problemContext.size()).order(byteOrder);

      contextPointer.set(problemContext);

      buildProblem.accept(this);

      CLBuffer<Integer> domainBuffer = context.createBuffer(Usage.Input, domainPointer);
      CLBuffer<Long> resultBuffer = context.createBuffer(Usage.InputOutput, resultPointer);
      CLBuffer<PermutationSizeContext> contextBuffer =
          context.createBuffer(Usage.InputOutput, contextPointer);

      CLProgram program =
          context.createProgram(
              "#define PERM_UNIQUE_ELEMENTS " + this.problemContext.uniqueElements() + "\n",
              IOUtils.readText(AbstractStructTest.class.getResource("./multiset_permutations.cl")));

      CLKernel expandKernel = program.createKernel("compute_permutation_sizes");
      expandKernel.setArgs(contextBuffer, domainBuffer, resultBuffer);

      CLEvent expandEvent = expandKernel.enqueueNDRange(queue, new int[] { problemContext.size() });

      resultPointer = resultBuffer.read(queue, expandEvent);

      assertResult.accept(this);

      context.release();
    }

    public void setDomain(int index, int... domain) {
      int pointerIndex = index * problemContext.uniqueElements();
      for (int d = 0; d < domain.length; d++) {
        domainPointer.set(pointerIndex + d, domain[d]);
      }
    }

    public Long getResult(int index) {
      return resultPointer.get(index);
    }
  }

  @Test
  public void computePermutationSizes() throws IOException {
    new PermutationSizeTable().withContext(new PermutationSizeContext().size(3).uniqueElements(6))
        .withBuildProblem(t -> {
          int index = 0;
          t.setDomain(index++, 1, 1, 1, 1, 1, 1);
          t.setDomain(index++, 1, 2, 1, 1, 1, 1);
          t.setDomain(index++, 1, 2, 1, 1, 1, 2);
        }).withAssertResult(t -> {
          int index = 0;
          assertThat(t.getResult(index++), equalTo(720L));
          assertThat(t.getResult(index++), equalTo(2520L));
          assertThat(t.getResult(index++), equalTo(10080L));
        }).doTest();
  }

  @Test
  public void computePermutationSizes3Elements() throws IOException {
    new PermutationSizeTable().withContext(new PermutationSizeContext().size(3).uniqueElements(3))
        .withBuildProblem(t -> {
          int index = 0;
          t.setDomain(index++, 1, 1, 1);
          t.setDomain(index++, 1, 2, 1);
          t.setDomain(index++, 1, 2, 2);
        }).withAssertResult(t -> {
          int index = 0;
          assertThat(t.getResult(index++), equalTo(6L));
          assertThat(t.getResult(index++), equalTo(12L));
          assertThat(t.getResult(index++), equalTo(30L));
        }).doTest();
  }
}
