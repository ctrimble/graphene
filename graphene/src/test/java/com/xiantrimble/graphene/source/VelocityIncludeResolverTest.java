package com.xiantrimble.graphene.source;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.xiantrimble.graphene.source.Source;

public class VelocityIncludeResolverTest {
	
	protected VelocityIncludeResolver resolver;
	
	@Before
	public void setUp() {
		resolver = VelocityIncludeResolver.builder()
				.withClassLoader(this.getClass().getClassLoader())
				.build();
	}

	@Test
	public void shouldRenderResource() throws Exception {
		String resource = "classpath:/com/xiantrimble/graphene/velocity_test.vm";
		Source source = resolver.resolve(resource, null);
		
		assertThat("the system id is set", source.getSystemId(), equalTo(resource));
		assertThat("the source was rendered", source.getSource(), equalTo("no methods\n"));
	}
	
	@Test
	public void shouldRenderResourceWithParam() throws Exception {
		String resource = "classpath:/com/xiantrimble/graphene/velocity_test.vm?method=name1";
		Source source = resolver.resolve(resource, null);
		
		assertThat("the system id is set", source.getSystemId(), equalTo(resource));
		assertThat("the source was rendered", source.getSource(), equalTo("method name1\n"));
	}

	@Test
	public void shouldRenderResourceWithMultivalueParam() throws Exception {
		String resource = "classpath:/com/xiantrimble/graphene/velocity_test.vm?method=name1&method=name2";
		Source source = resolver.resolve(resource, null);
		
		assertThat("the system id is set", source.getSystemId(), equalTo(resource));
		assertThat("the source was rendered", source.getSource(), equalTo("method name1\nmethod name2\n"));
	}

	@Test
	public void shouldRenderResourceWithEncodedParam() throws Exception {
		String resource = "classpath:/com/xiantrimble/graphene/velocity_test.vm?method=name%3D";
		Source source = resolver.resolve(resource, null);
		
		assertThat("the system id is set", source.getSystemId(), equalTo(resource));
		assertThat("the source was rendered", source.getSource(), equalTo("method name=\n"));
	}


}
