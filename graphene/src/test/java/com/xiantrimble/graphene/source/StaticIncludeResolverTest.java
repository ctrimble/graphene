package com.xiantrimble.graphene.source;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import org.junit.Before;
import org.junit.Test;

import com.xiantrimble.graphene.source.StaticIncludeResolver;
import com.xiantrimble.graphene.source.Source;

public class StaticIncludeResolverTest {
	
	StaticIncludeResolver resolver;
	
	@Before
	public void setUp() {
		resolver = StaticIncludeResolver.builder().build();
	}

	@Test
	public void shouldResolveAbsoluteWithoutBase() throws Exception {
		Source source = resolver.resolve("/com/xiantrimble/graphene/resource_test_file.cl", null);

	  assertThat(source.getSystemId(), equalTo("classpath:/com/xiantrimble/graphene/resource_test_file.cl"));
	  assertThat(source.getSource(), equalTo("Test Resource"));
	}

	@Test
	public void shouldResolveAbsoluteWithBase() throws Exception {
		Source source = resolver.resolve(
				"classpath:/com/xiantrimble/graphene/resource_test_file.cl",
				"unknown:/some/unknown/path");

	  assertThat(source.getSystemId(), equalTo("classpath:/com/xiantrimble/graphene/resource_test_file.cl"));
	  assertThat(source.getSource(), equalTo("Test Resource"));
	}
	
	@Test
	public void shouldResolveRelativePath() throws Exception {
		Source source = resolver.resolve(
				"./resource_test_file.cl",
				"classpath:/com/xiantrimble/graphene/context.cl");

	  assertThat(source.getSystemId(), equalTo("classpath:/com/xiantrimble/graphene/resource_test_file.cl"));
	  assertThat(source.getSource(), equalTo("Test Resource"));
	}
}
