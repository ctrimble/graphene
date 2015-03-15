package com.xiantrimble.graphene;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

public class CustomVertex extends AbstractVertexStruct<CustomValue> {
  public CustomVertex() {
  }

  public CustomVertex(Pointer<? extends StructObject> pointer) {
    super(pointer);
  }

  @Field(VALUE_INDEX)
  public CustomValue value() {
    return this.io.getNativeObjectField(this, VALUE_INDEX);
  };

  @Field(VALUE_INDEX)
  public AbstractVertexStruct<CustomValue> value(CustomValue data) {
    this.io.setNativeObjectField(this, VALUE_INDEX, data);
    return this;
  };

  @Field(INITIAL_VALUE_INDEX)
  public CustomValue initialValue() {
    return this.io.getNativeObjectField(this, INITIAL_VALUE_INDEX);
  };

  @Field(INITIAL_VALUE_INDEX)
  public AbstractVertexStruct<CustomValue> initialValue(CustomValue initialValue) {
    this.io.setNativeObjectField(this, INITIAL_VALUE_INDEX, initialValue);
    return this;
  };
}