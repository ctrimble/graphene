package com.xiantrimble.graphene;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

public abstract class EdgeConstructionContext<P extends StructObject, C extends EdgeConstructionContext<P, C>> extends StructObject {
	public static final int RANGE_INDEX = 0;
	public static final int PARTITION_INDEX = 1;
	public static final int EDGE_INDEX_INDEX = 2;
	public static final int EDGES_LENGTH_INDEX = 3;
	public static final int OVERFLOW_INDEX = 4;
  public EdgeConstructionContext() {
  }

  public EdgeConstructionContext(Pointer<? extends StructObject> pointer) {
    super(pointer);
  }
  
  @SuppressWarnings("unchecked")
  public C thisObject() {
  	return (C)this;
  }

  @Field(RANGE_INDEX)
  public IndexRange range() {
    return this.io.getNativeObjectField(this, RANGE_INDEX);
  };

  @Field(RANGE_INDEX)
  public C range(IndexRange range) {
    this.io.setNativeObjectField(this, RANGE_INDEX, range);
    return thisObject();
  };

  @Field(RANGE_INDEX)
  public abstract P partition();

  @Field(RANGE_INDEX)
  public abstract C partition(P range);
  
  @Field(EDGE_INDEX_INDEX)
  public int edgeIndex() {
    return this.io.getIntField(this, EDGE_INDEX_INDEX);
  };

  @Field(EDGE_INDEX_INDEX)
  public C edgeIndex(int edgeIndex) {
    this.io.setIntField(this, EDGE_INDEX_INDEX, edgeIndex);
    return thisObject();
  };
  
  @Field(EDGES_LENGTH_INDEX)
  public int edgesLength() {
    return this.io.getIntField(this, EDGES_LENGTH_INDEX);
  };

  @Field(EDGES_LENGTH_INDEX)
  public C edgesLength(int edgesLength) {
    this.io.setIntField(this, EDGES_LENGTH_INDEX, edgesLength);
    return thisObject();
  };	  
  @Field(OVERFLOW_INDEX)
  public int overflowIndex() {
    return this.io.getIntField(this, OVERFLOW_INDEX);
  };

  @Field(OVERFLOW_INDEX)
  public C overflowIndex(int overflowIndex) {
    this.io.setIntField(this, OVERFLOW_INDEX, overflowIndex);
    return thisObject();
  };
}

