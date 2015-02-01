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
package com.xiantrimble.graphene.example.commands;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import org.bridj.Pointer;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.WorkHandler;
import com.nativelibs4java.opencl.CLDevice;

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;

/**
 * Provides a command that outputs all combinations of a set of elements.
 * 
 * # Command Line
 * 
 * graphene-example combinations -s [elements] -k size
 * @author Christian Trimble
 *
 */
public class CombinationsCommand extends Command {

  public CombinationsCommand() {
    super("combinations", "generates combinations for a set");
  }

  @Override
  public void configure(Subparser subparser) {
    subparser.addArgument("set").action(Arguments.uniqueSortedList()).help("the set to combine");
    subparser.addArgument("--size", "-k").type(Integer.class)
        .help("the number of elements in each combination");
  }

  @Override
  public void run(Bootstrap<?> bootstrap, Namespace ns) throws Exception {
    final String[] elements = ns.<List<String>>get("set").toArray(new String[0]);
    final Integer inputSize = ns.getInt("size");

    if (inputSize > elements.length) {
      System.err.println("size larger than set");
      System.exit(1);
    }
    final Integer size = inputSize != null ? inputSize : elements.length;
    
  	final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new
        FileOutputStream(java.io.FileDescriptor.out), "ASCII"), 2056);

    CombinatoricEnumerator.<CombinatoricEvent>builder()
    	  .withEventFactory(CombinatoricEvent::new)
				.withRangeGetter(CombinatoricEvent::getRange)
				.withRangeSetter(CombinatoricEvent::setRange)
				.withElementGetter(CombinatoricEvent::getElements)
				.withElementSetter(CombinatoricEvent::setElements)
        .withElements(elements)
        .withK(size)
        .build()
        .enumerate((e, s, b) -> {
          Range<Long> range = e.getRange();
          Pointer<Integer> elementPointer = e.getElements();
          
          String[] result = new String[size];
          int pageSize = (int)(range.upperEndpoint() - range.lowerEndpoint());
          for (int i = 0; i < pageSize; i++) {
            for (int j = 0; j < size; j++) {
            	int index = elementPointer.get((i * size) + j);
            	result[j] = elements[index];
            }
            out.write(Arrays.toString(result));
            out.write("\n");
        	}
          elementPointer.release();
        });
    out.close();

    // at this point, we have the set and number to generate. Create
    // a combination component and stream output out.
  }

  @SuppressWarnings("unchecked")
  public static <T> WorkHandler<T>[] createPool(int size, IntFunction<WorkHandler<T>> creator) {
    return IntStream.range(0, size).mapToObj(creator).toArray(WorkHandler[]::new);
  }

  public static Stream<Range<Long>> rangeStream(Range<Long> range, int maxRangeSize) {
    return StreamSupport.stream(rangeIterable(range, maxRangeSize).spliterator(), false);
  }

  public static Iterable<Range<Long>> rangeIterable(final Range<Long> range, int segmentLength) {
    return () -> {
      return new Iterator<Range<Long>>() {
        long nextStart = range.lowerEndpoint() + (range.lowerBoundType() == BoundType.OPEN ? 1 : 0);
        long end = range.upperEndpoint() + (range.upperBoundType() == BoundType.OPEN ? 0 : 1);

        @Override
        public boolean hasNext() {
          return range.contains(nextStart);
        }

        @Override
        public Range<Long> next() {
          if (nextStart == end)
            throw new NoSuchElementException("no more ranges");
          return Range.closedOpen(nextStart, nextStart = Math.min(nextStart + segmentLength, end));
        }
      };
    };
  }

  public static class CombinatoricEvent {
    public Range<Long> range;
    public Pointer<Integer> elements;
    
    public void setRange( Range<Long> range ) {
    	this.range = range;
    }
    
    public Range<Long> getRange() {
    	return this.range;
    }
    public void setElements( Pointer<Integer> elements ) {
    	this.elements = elements;
    }
    
    public Pointer<Integer> getElements() {
    	return this.elements;
    }
  }

}
