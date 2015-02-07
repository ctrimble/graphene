package com.xiantrimble.graphene;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

public class DisruptorUtils {
  public static void shutdownDisruptor(Disruptor<?> disruptor) {
    try {
      RingBuffer<?> buffer = disruptor.getRingBuffer();
      while( buffer.getBufferSize() != buffer.remainingCapacity() ) {
  	      Thread.sleep(50);
      }
    }
    catch( InterruptedException ie ) {
    	// could not wait for halt...
    }

    disruptor.halt();
    
    disruptor.shutdown();
  }
}
