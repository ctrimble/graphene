package com.xiantrimble.graphene.source;

import java.util.List;

import com.google.common.collect.Lists;

public class ResolverChain<T> implements Resolver<T> {
	
	public static class Builder<T> {
		protected List<Resolver<T>> resolvers = Lists.newArrayList();
		
		public Builder<T> addResolver( Resolver<T> resolver ) {
			resolvers.add(resolver);
			return this;
		}
		
		public ResolverChain<T> build() {
			return new ResolverChain<T>(resolvers);
		}
	}
	
	public static <T> Builder<T> builder() {
		return new Builder<T>();
	}
	
	protected List<Resolver<T>> resolvers;
	
	private ResolverChain( List<Resolver<T>> resolvers ) {
		this.resolvers = resolvers;
	}

	@Override
  public T resolve(String href, String base) {
		return resolvers.stream()
				.map(r->r.resolve(href, base))
				.filter(s->s!=null)
				.findFirst()
				.orElse(null);
  }

}
