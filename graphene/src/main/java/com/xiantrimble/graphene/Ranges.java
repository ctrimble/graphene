package com.xiantrimble.graphene;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

public class Ranges {
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
  
  public static Stream<IndexRange> rangeStream(IndexRange range, int maxRangeSize) {
    return StreamSupport.stream(rangeIterable(range, maxRangeSize).spliterator(), false);
  }

  public static Iterable<IndexRange> rangeIterable(final IndexRange range, int segmentLength) {
    return () -> {
      return new Iterator<IndexRange>() {
        long nextStart = range.start();
        long end = range.end();

        @Override
        public boolean hasNext() {
          return range.contains(nextStart);
        }

        @Override
        public IndexRange next() {
          if (nextStart == end)
            throw new NoSuchElementException("no more ranges");
          return new IndexRange().start(nextStart).end(nextStart = Math.min(nextStart + segmentLength, end));
        }
      };
    };
  }

}
