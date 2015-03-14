package com.xiantrimble.graphene.source;

public interface Resolver<T> {

  public T resolve( String href, String base );
}
