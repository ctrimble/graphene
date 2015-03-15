package com.xiantrimble.graphene.example.commands;

import java.util.function.Consumer;

import org.bridj.Pointer;

/**
 * Builds a graph, based on index ranges.  The basic process is:
 * 
 * 1. expand a range into a set of permutations.
 * 2. map the permutation into a set of next permutations.
 * 3. map the next permutations into their dictionary order index.
 * 4. use atomic operations to write the edges into an array.
 * 5. download the edges.
 * 
 * @author Christian Trimble
 *
 */
public class InternalEdgeBuilder<E> {
  public void buildGraph( Consumer<Pointer<Long>> consumer ) {
  	
  }
  
}
