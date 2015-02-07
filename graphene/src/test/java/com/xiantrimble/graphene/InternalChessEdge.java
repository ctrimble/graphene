package com.xiantrimble.graphene;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

public class InternalChessEdge extends AbstractEdgeStruct<ChessValue> {
  public InternalChessEdge() {
  }

  public InternalChessEdge(Pointer<? extends StructObject> pointer) {
    super(pointer);
  }

  @Field(TO_VALUE_INDEX)
  public ChessValue toValue() {
    return this.io.getNativeObjectField(this, TO_VALUE_INDEX);
  };

  @Field(TO_VALUE_INDEX)
  public AbstractEdgeStruct<ChessValue> toValue(ChessValue toValue) {
    this.io.setNativeObjectField(this, TO_VALUE_INDEX, toValue);
    return this;
  };
}