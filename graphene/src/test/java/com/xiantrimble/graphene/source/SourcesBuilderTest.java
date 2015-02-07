package com.xiantrimble.graphene.source;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import org.junit.Before;
import org.junit.Test;

public class SourcesBuilderTest {
	
	SourcesBuilder builder;
	
	@Before
	public void setUp() {
		builder = SourcesBuilder.builder()
				.withBaseUri("classpath:/com/xiantrimble/graphene/source/")
				.withIncludeResolver(StaticIncludeResolver.builder().build())
				.build();
	}

	@Test
	public void shouldResolveInlineSource() {
		String[] actual = builder
		.addSource("#include \"classpath:/com/xiantrimble/graphene/source/preprocessor_test_no_includes.cl\"")
		.build();
		
		assertThat(actual.length, equalTo(1));
		assertThat(actual[0], equalTo(
				"#line 1 \"source:0\"\n"+
		    "#line 1 \"classpath:/com/xiantrimble/graphene/source/preprocessor_test_no_includes.cl\"\n"+
				"include 1 content\n"+
		    "#line 2 \"source:0\"\n"));
	}

}
