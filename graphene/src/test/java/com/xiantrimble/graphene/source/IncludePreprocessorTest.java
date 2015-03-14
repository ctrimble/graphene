package com.xiantrimble.graphene.source;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import org.junit.Before;
import org.junit.Test;

import com.xiantrimble.graphene.source.Source;
import com.xiantrimble.graphene.source.StaticIncludeResolver;

public class IncludePreprocessorTest {
	IncludePreprocessor preprocessor;
	Resolver<Source> resolver = StaticIncludeResolver.builder().build();
	
	@Before
	public void setUp() {
		preprocessor = IncludePreprocessor.builder()
				.withResolver(resolver)
				.build();
	}

	@Test
	public void shouldProcessSingleInclude() {
		Source before = resolver.resolve("classpath:/com/xiantrimble/graphene/source/preprocessor_test_single_include.cl", null);
		Source after = preprocessor.preprocess(before);
		
		assertThat(after.getSource(), equalTo(
				"#line 1 \"classpath:/com/xiantrimble/graphene/source/preprocessor_test_single_include.cl\"\n"+
		    "#line 1 \"classpath:/com/xiantrimble/graphene/source/preprocessor_test_no_includes.cl\"\n"+
				"include 1 content\n"+
		    "#line 2 \"classpath:/com/xiantrimble/graphene/source/preprocessor_test_single_include.cl\"\n"));
	}
	
	@Test
	public void shouldMatchIncludeLine() {
		assertThat(IncludePreprocessor
				.includePattern.matcher("#include \"test\"").matches(), equalTo(true));
	}

}
