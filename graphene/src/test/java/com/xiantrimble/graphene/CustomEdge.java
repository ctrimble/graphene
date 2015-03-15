package com.xiantrimble.graphene;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

public class CustomEdge extends AbstractEdgeStruct<CustomValue> {
  public CustomEdge() {
  }

  public CustomEdge(Pointer<? extends StructObject> pointer) {
    super(pointer);
  }

  @Field(TO_VALUE_INDEX)
  public CustomValue toValue() {
    return this.io.getNativeObjectField(this, TO_VALUE_INDEX);
  };

  @Field(TO_VALUE_INDEX)
  public AbstractEdgeStruct<CustomValue> toValue(CustomValue toValue) {
    this.io.setNativeObjectField(this, TO_VALUE_INDEX, toValue);
    return this;
  };
}