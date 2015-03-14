package com.xiantrimble.graphene.source;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * Builds the sources array to pass to JavaCL.
 * 
 * @author Christian Trimble
 *
 */
public class SourcesBuilder {
	
	public static class Builder {
		protected Resolver<Source> resolver;
		protected String base;
		
		public Builder withIncludeResolver( Resolver<Source> resolver ) {
			this.resolver = resolver;
			return this;
		}
		
		public Builder withBaseUri( String base ) {
			this.base = base;
			return this;
		}
		
		public SourcesBuilder build() {
			return new SourcesBuilder(resolver, base);
		}
	}
	
	protected Resolver<Source> resolver;
	protected String base;
	protected List<Source> sources = Lists.newArrayList();
	
	public SourcesBuilder(Resolver<Source> resolver, String base) {
	  this.resolver = resolver;
	  this.base = base;
  }

	public static Builder builder() {
		return new Builder();
	}
	
	public SourcesBuilder addUri( String href ) {
		Source source = resolver.resolve(href, base);
		if( source == null ) {
			throw new SourceResolutionException(String.format("could not find source for %s.", href));
		}
		sources.add(source);
	  return this;	
	}
	
	public SourcesBuilder addSource( String source ) {
		sources.add(new Source()
		  .withSystemId("source:"+sources.size())
		  .withSource(source));
		return this;
	}
	
  public String[] build() {
  	IncludePreprocessor preprocessor = IncludePreprocessor.builder()
  			.withResolver(resolver)
  			.build();
  	
  	return sources.stream()
  			.map(preprocessor::preprocess)
  			.map(Source::getSource)
  			.toArray(String[]::new);
  }
}
