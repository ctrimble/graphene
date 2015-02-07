package com.xiantrimble.graphene.source;

public class IncludeResolvers {
  public static Resolver<Source> defaultResolver() {
  	return ResolverChain.<Source>builder()
  			.addResolver(VelocityIncludeResolver.builder()
  					.withClassLoader(IncludeResolvers.class.getClassLoader())
  					.build())
  			.addResolver(StaticIncludeResolver.builder().build())
  					.build();
  }
}
