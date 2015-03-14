package com.xiantrimble.graphene.source;

import static java.lang.String.format;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import com.google.common.collect.Maps;

public class VelocityIncludeResolver implements Resolver<Source> {
	
	public static class Builder {
		protected Resolver<URI> resolver;
		protected Predicate<URI> isVelocity;
		private ClassLoader loader;
		
		public Builder withUriResolver( Resolver<URI> resolver ) {
			this.resolver = resolver;
			return this;
		}
		
		public Builder withIsVelocity( Predicate<URI> isVelocity ) {
			this.isVelocity = isVelocity;
			return this;
		}
		
		public Builder withClassLoader( ClassLoader loader ) {
			this.loader = loader;
			return this;
		}
		
		public VelocityIncludeResolver build() {
			if( resolver == null ) {
				resolver = Uris.DEFAULT_RESOLVER;
			}
			if( isVelocity == null ) {
				isVelocity = Uris.hasExtension("vm");
			}
			if( loader == null ) {
				throw new IllegalArgumentException("Loader required.");
			}
			return new VelocityIncludeResolver(loader, resolver, isVelocity);
		}
	}

	public static Builder builder() {
	  return new Builder();
  }
	
	VelocityEngine ve = new VelocityEngine();
	Resolver<URI> resolver;
	Predicate<URI> isVelocity;
	
	public VelocityIncludeResolver( ClassLoader loader, Resolver<URI> resolver, Predicate<URI> isVelocity )
	{
		this.resolver = resolver;
		this.isVelocity = isVelocity;
		ExtendedProperties extended = new ExtendedProperties();
		extended.setProperty(VelocityEngine.RESOURCE_LOADER, "graphene");
		extended.setProperty("graphene.resource.loader.instance", new ClassLoaderResourceLoader(loader));
	  ve.setExtendedProperties(extended);
		ve.init();
	}

	@Override
  public Source resolve(String href, String base) {
	  URI resourceUri = resolver.resolve(href, base);
	  
	  if( !isVelocity.test(resourceUri) ) {
	  	return null;
	  }
	  
	  if( "classpath".equals(resourceUri.getScheme()) ) {
	  	Template template = ve.getTemplate(resourceUri.getPath().replaceAll("^/", ""));
	  	
	  	// build the context.
	  	VelocityContext context = new VelocityContext();
	  	parseQueryString(resourceUri.getQuery())
	  	  .entrySet().stream()
	  	  .forEach(e->context.put(e.getKey(), e.getValue()));
	  	
	  	StringWriter writer = new StringWriter();
	  	template.merge(context, writer);
	  	
	  	return new Source()
	  	  .withSystemId(resourceUri.toString())
	  	  .withSource(writer.toString());
	  }
	  else {
	  	return null;
	  }
  }
	
	public static Map<String, List<String>> parseQueryString( String queryString ) {
		if( queryString == null ) { return Maps.newLinkedHashMap(); }
		return Stream.of(queryString.split("&"))
	  .map(param->param.split("=", 2))
	  .collect(Collectors.groupingBy(
	  		param->decode(param[0], "UTF-8"),
	  		Collectors.mapping(param->decode(param[1], "UTF-8"), Collectors.toList())));
	}
	
	public static String decode( String value, String enc ) {
			try {
	      return URLDecoder.decode(value, enc);
      } catch (UnsupportedEncodingException e) {
	      throw new RuntimeException(format("could not decode %s with encoding %s", value, enc), e);
      }
	}
	
	public static class ClassLoaderResourceLoader extends ResourceLoader {
		
		protected long lastModified = System.currentTimeMillis();
		protected ClassLoader classLoader;

		public ClassLoaderResourceLoader(ClassLoader loader) {
	    this.classLoader = loader;
    }

		@Override
    public long getLastModified(Resource arg0) {
	    return lastModified;
    }

		@Override
    public InputStream getResourceStream(String source)
        throws ResourceNotFoundException {
	    return classLoader.getResourceAsStream(source);
    }

		@Override
    public void init(ExtendedProperties properties) {
    }

		@Override
    public boolean isSourceModified(Resource resource) {
	    return false;
    }
		
	}

}
