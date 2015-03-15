package com.xiantrimble.graphene;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

public class CustomValue extends StructObject {
  public static final int RESULT_INDEX = 0;
  public static final int DEPTH_INDEX = 1;

  public CustomValue() {
  }

  public CustomValue(Pointer<? extends StructObject> pointer) {
    super(pointer);
  }

  @Field(RESULT_INDEX)
  public int result() {
    return this.io.getIntField(this, RESULT_INDEX);
  }

  @Field(RESULT_INDEX)
  public CustomValue result(int result) {
    this.io.setIntField(this, RESULT_INDEX, result);
    return this;
  }

  @Field(DEPTH_INDEX)
  public int depth() {
    return this.io.getIntField(this, DEPTH_INDEX);
  }

  @Field(DEPTH_INDEX)
  public CustomValue depth(int depth) {
    this.io.setIntField(this, DEPTH_INDEX, depth);
    return this;
  }
}