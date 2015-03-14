package com.xiantrimble.graphene.source;

public class Source {
  protected String systemId;
  protected String source;
  
  public Source withSystemId( String systemId ) {
  	this.systemId = systemId;
  	return this;
  }
  
  public String getSystemId() {
  	return this.systemId;
  }
  
  public Source withSource( String source ) {
  	this.source = source;
  	return this;
  }
  
  public String getSource() {
  	return this.source;
  }
}
