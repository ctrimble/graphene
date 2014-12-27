package com.xiantrimble.graphene;

/**
 * Entry point for the library.
 * 
 * @author Christian Trimble
 *
 */
public class Graphene {
  public static class Builder {
   
    public Graphene build() {
      return new Graphene();
    }
  }
  
  public static Builder builder() {
    return new Builder();
  }
  
  Graphene() {
    
  }
}
