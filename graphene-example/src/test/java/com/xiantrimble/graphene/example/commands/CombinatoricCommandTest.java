package com.xiantrimble.graphene.example.commands;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.bridj.Pointer;
import org.junit.Test;

import com.google.common.collect.Range;
import com.xiantrimble.graphene.example.commands.CombinationsCommand.CombinatoricEvent;

public class CombinatoricCommandTest {

	@Test
	public void test() throws InterruptedException, UnsupportedEncodingException {
		CombinatoricEnumerator<CombinatoricEvent> enumerator = CombinatoricEnumerator.<CombinatoricEvent>builder()
				.withEventFactory(CombinatoricEvent::new)
				.withRangeGetter(CombinatoricEvent::getRange)
				.withRangeSetter(CombinatoricEvent::setRange)
				.withElementGetter(CombinatoricEvent::getElements)
				.withElementSetter(CombinatoricEvent::setElements)
		  .withElements("one", "two", "three")
		  .withK(2)
		  .build();
		
		enumerator.enumerate((e, s, b) -> {
      Range<Long> range = e.getRange();
      Pointer<Integer> elementPointer = e.getElements();
      elementPointer.release();
    });
	}

}
