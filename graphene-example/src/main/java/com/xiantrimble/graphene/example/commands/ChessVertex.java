package com.xiantrimble.graphene.example.commands;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

import com.xiantrimble.graphene.AbstractVertexStruct;

public class ChessVertex extends AbstractVertexStruct<ChessValue> {
  public ChessVertex() {
  }

  public ChessVertex(Pointer<? extends StructObject> pointer) {
    super(pointer);
  }

  @Field(VALUE_INDEX)
  public ChessValue value() {
    return this.io.getNativeObjectField(this, VALUE_INDEX);
  };

  @Field(VALUE_INDEX)
  public AbstractVertexStruct<ChessValue> value(ChessValue data) {
    this.io.setNativeObjectField(this, VALUE_INDEX, data);
    return this;
  };

  @Field(INITIAL_VALUE_INDEX)
  public ChessValue initialValue() {
    return this.io.getNativeObjectField(this, INITIAL_VALUE_INDEX);
  };

  @Field(INITIAL_VALUE_INDEX)
  public AbstractVertexStruct<ChessValue> initialValue(ChessValue initialValue) {
    this.io.setNativeObjectField(this, INITIAL_VALUE_INDEX, initialValue);
    return this;
  };
}