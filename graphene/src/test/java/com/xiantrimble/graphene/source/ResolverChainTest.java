package com.xiantrimble.graphene.source;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ResolverChainTest {

	Resolver<Source> firstResolver;
	Resolver<Source> secondResolver;

	ResolverChain<Source> chain;

	public static Source SOURCE_1 = new Source().withSystemId("classpath:/path1")
	    .withSource("source 1");

	public static Source SOURCE_2 = new Source().withSystemId("classpath:/path2")
	    .withSource("source 2");

	@SuppressWarnings("unchecked")
  @Before
	public void setUp() {
		firstResolver = mock(Resolver.class);
		secondResolver = mock(Resolver.class);

		chain = ResolverChain.<Source>builder()
				.addResolver(firstResolver)
		    .addResolver(secondResolver)
		    .build();
	}

	@Test
	public void shouldReturnFirstInstance() {
		when(firstResolver.resolve(any(String.class), any(String.class)))
		    .thenReturn(SOURCE_1);
		when(secondResolver.resolve(any(String.class), any(String.class)))
		    .thenReturn(SOURCE_2);

		Source actual = chain.resolve("path1", "classpath:/");

		assertThat(actual, equalTo(SOURCE_1));
	}

	@Test
	public void shouldNotCallExtraResolvers() {
		when(firstResolver.resolve(any(String.class), any(String.class)))
		    .thenReturn(SOURCE_1);
		when(secondResolver.resolve(any(String.class), any(String.class)))
		    .thenReturn(SOURCE_2);

		chain.resolve("path1", "classpath:/");

		verify(firstResolver, times(1)).resolve(any(String.class),
		    any(String.class));
		verify(secondResolver, never()).resolve(any(String.class),
		    any(String.class));
	}

	@Test
	public void shouldReturnSecondResultWhenFirstNull() {
		when(firstResolver.resolve(any(String.class), any(String.class)))
		    .thenReturn(null);
		when(secondResolver.resolve(any(String.class), any(String.class)))
		    .thenReturn(SOURCE_2);

		Source actual = chain.resolve("path1", "classpath:/");

		assertThat(actual, equalTo(SOURCE_2));
	}

	@Test
	public void shouldTryAllResolversWhenNotFound() {
		when(firstResolver.resolve(any(String.class), any(String.class)))
		    .thenReturn(null);
		when(secondResolver.resolve(any(String.class), any(String.class)))
		    .thenReturn(null);

		chain.resolve("path1", "classpath:/");

		verify(firstResolver, times(1)).resolve(any(String.class),
		    any(String.class));
		verify(secondResolver, times(1)).resolve(any(String.class),
		    any(String.class));
	}

	@Test
	public void shouldReturnNullWhenNotFound() {
		when(firstResolver.resolve(any(String.class), any(String.class)))
		    .thenReturn(null);
		when(secondResolver.resolve(any(String.class), any(String.class)))
		    .thenReturn(null);

		Source actual = chain.resolve("path1", "classpath:/");

		assertThat("the result is null", actual, nullValue());
	}

	@Test
	public void shouldReturnNullForEmptyChain() {
		Source actual = ResolverChain.<Source>builder().build()
		    .resolve("path1", "classpath:/");

		assertThat("the result is null", actual, nullValue());
	}
	
	@Test(expected=RuntimeException.class)
	public void shouldPropigateExceptions() {
		when(firstResolver.resolve(any(String.class), any(String.class)))
      .thenThrow(new RuntimeException());
    when(secondResolver.resolve(any(String.class), any(String.class)))
      .thenReturn(null);

    chain.resolve("path1", "classpath:/");

	}
}
