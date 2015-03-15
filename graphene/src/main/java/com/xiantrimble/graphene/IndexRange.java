package com.xiantrimble.graphene;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

public class IndexRange extends StructObject {
  public static final int START_INDEX = 0;
  public static final int END_INDEX = 1;

  public IndexRange() {
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public IndexRange(Pointer pointer) {
    super(pointer);
  }

  @Field(START_INDEX)
  public long start() {
    return this.io.getLongField(this, START_INDEX);
  };

  @Field(START_INDEX)
  public IndexRange start(long start) {
    this.io.setLongField(this, START_INDEX, start);
    return this;
  };

  @Field(END_INDEX)
  public long end() {
    return this.io.getLongField(this, END_INDEX);
  };

  @Field(END_INDEX)
  public IndexRange end(long end) {
    this.io.setLongField(this, END_INDEX, end);
    return this;
  };
  
  public boolean contains(long index) {
  	return start()<=index && end()>index;
  }

}