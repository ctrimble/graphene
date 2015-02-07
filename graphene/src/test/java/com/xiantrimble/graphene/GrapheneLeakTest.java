package com.xiantrimble.graphene;

import org.junit.Test;

/**
 * A test that just creates and closes Graphene repeatedly, to make sure the
 * JavaCL classes are being released.
 * 
 * @author Christian Trimble
 *
 */
public class GrapheneLeakTest {
	@Test
  public void shouldNotLeak() {
  	for( int i = 0; i < 100; i++ ) {
  		Graphene.builder().build().close();
  	}
  }
}
