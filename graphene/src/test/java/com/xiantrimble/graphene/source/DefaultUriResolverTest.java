package com.xiantrimble.graphene.source;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;

public class DefaultUriResolverTest {
	
	Resolver<URI> resolver;
	
	@Before
	public void setUp() {
		resolver = Uris.DEFAULT_RESOLVER;
	}

	@Test
	public void shouldResolveAbsolutePathWithoutBase() {
		URI actual = resolver.resolve("/path", null);
		assertThat(actual, equalTo(URI.create("classpath:/path")));
	}
	
	@Test
	public void shouldResolveAbsolutePathWithBase() {
		URI actual = resolver.resolve("/path", "classpath:/some/other-path");
		assertThat(actual, equalTo(URI.create("classpath:/path")));
	}
	
	@Test
	public void shouldResolveRelativeWithoutBase() {
		URI actual = resolver.resolve("./path", null);
		assertThat(actual, equalTo(URI.create("classpath:/path")));		
	}
	
	@Test
	public void shouldResolveRelativeWithBase() {
		URI actual = resolver.resolve("./path", "classpath:/some/other-path");
		assertThat(actual, equalTo(URI.create("classpath:/some/path")));		
	}

}
