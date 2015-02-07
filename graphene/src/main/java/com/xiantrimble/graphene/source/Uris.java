package com.xiantrimble.graphene.source;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.net.URI;
import java.util.function.Predicate;

public class Uris {
	public static Resolver<URI> DEFAULT_RESOLVER = defaultResolver("classpath:/");
	
	public static Resolver<URI> defaultResolver( String defaultBase ) {
		return (href, base)->{
			return (isNullOrEmpty(base) ?
		  		URI.create(defaultBase) :
		      URI.create(base))
		  .resolve(href);
		};
	}
	
	public static Predicate<URI> hasExtension( String extension ) {
		return (uri)->{
			return uri.getPath().endsWith("."+extension);
		};
	}

	public static Predicate<URI> anyPath() {
	  return (uri)->{
	  	return true;
	  };
  }
	
	public static String uriStringFor( Class<?> clazz ) {
		return String.format("classpath:/%s.class", clazz.getName().replaceAll("\\.", "/"));
	}
}
