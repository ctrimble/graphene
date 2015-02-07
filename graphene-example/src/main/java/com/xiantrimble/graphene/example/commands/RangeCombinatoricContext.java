package com.xiantrimble.graphene.example.commands;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

public class RangeCombinatoricContext extends StructObject {
	public static final int RANGE_INDEX = 0;
	public static final int UNIQUE_ELEMENTS_INDEX = 1;
	public static final int SUBSET_SIZE = 2;
	
  public RangeCombinatoricContext() {
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public RangeCombinatoricContext(Pointer pointer) {
    super(pointer);
  }
  
  @Field(RANGE_INDEX)
  public IndexRange range() {
    return this.io.getNativeObjectField(this, RANGE_INDEX);
  };

  @Field(RANGE_INDEX)
  public RangeCombinatoricContext range(IndexRange range) {
    this.io.setNativeObjectField(this, RANGE_INDEX, range);
    return this;
  };
  
  @Field(UNIQUE_ELEMENTS_INDEX)
  public int uniqueElements() {
    return this.io.getIntField(this, UNIQUE_ELEMENTS_INDEX);
  };

  @Field(UNIQUE_ELEMENTS_INDEX)
  public RangeCombinatoricContext uniqueElements(int uniqueElements) {
    this.io.setIntField(this, UNIQUE_ELEMENTS_INDEX, uniqueElements);
    return this;
  };
  
  @Field(SUBSET_SIZE)
  public int subsetSize() {
    return this.io.getIntField(this, SUBSET_SIZE);
  };

  @Field(SUBSET_SIZE)
  public RangeCombinatoricContext subsetSize(int subsetSize) {
    this.io.setIntField(this, SUBSET_SIZE, subsetSize);
    return this;
  };
}