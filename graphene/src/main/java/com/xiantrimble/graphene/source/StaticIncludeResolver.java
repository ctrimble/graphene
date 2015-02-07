package com.xiantrimble.graphene.source;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.function.Predicate;

import com.google.common.io.Resources;

public class StaticIncludeResolver implements Resolver<Source> {

	public static class Builder {
		protected Resolver<URI> resolver;
		protected Predicate<URI> isStatic;
		
		public Builder withUriResolver( Resolver<URI> resolver ) {
			this.resolver = resolver;
			return this;
		}
		
		public Builder withIsStatic( Predicate<URI> isStatic ) {
			this.isStatic = isStatic;
			return this;
		}
		
		public StaticIncludeResolver build() {
			if( resolver == null ) {
				resolver = Uris.DEFAULT_RESOLVER;
			}
			if( isStatic == null ) {
				isStatic = Uris.anyPath();
			}
			return new StaticIncludeResolver(resolver, isStatic);
		}
	}

	public static Builder builder() {
	  return new Builder();
  }

	private Resolver<URI> resolver;
	private Predicate<URI> isStatic;
	
	protected StaticIncludeResolver(Resolver<URI> resolver, Predicate<URI> isStatic) {
		this.resolver = resolver;
		this.isStatic = isStatic;
	}
	
	@Override
  public Source resolve(String href, String base) {
	  URI resourceUri = resolver.resolve(href, base);
	  
	  if( !isStatic.test(resourceUri) ) {
	  	return null;
	  }
	  
	  if( "classpath".equals(resourceUri.getScheme()) ) {
	  	return new Source()
	  	  .withSystemId(resourceUri.toString())
	  	  .withSource(loadResource(this.getClass(), resourceUri.getPath()));
	  }
	  else {
	  	return null;
	  }
  }
	
	private static String loadResource( Class<?> context, String path ) {
		try {
	    return Resources.toString(
	    		Resources.getResource(context, path),
	    		Charset.forName("UTF-8"));
    } catch (IOException e) {
	    throw new SourceResolutionException("could not load resource at "+path, e);
    }
	}

}
