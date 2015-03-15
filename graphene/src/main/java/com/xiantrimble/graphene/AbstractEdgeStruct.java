package com.xiantrimble.graphene;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

public abstract class AbstractEdgeStruct<D extends StructObject> extends StructObject {
  public static final int TO_INDEX = 0;
  public static final int FROM_INDEX = 1;
  public static final int TO_VALUE_INDEX = 2;

  public AbstractEdgeStruct() {
  }

  public AbstractEdgeStruct(Pointer<? extends StructObject> pointer) {
    super(pointer);
  }

  @Field(TO_INDEX)
  public long to() {
    return this.io.getLongField(this, TO_INDEX);
  };

  @Field(TO_INDEX)
  public AbstractEdgeStruct<D> to(long in) {
    this.io.setLongField(this, TO_INDEX, in);
    return this;
  };

  @Field(FROM_INDEX)
  public long from() {
    return this.io.getLongField(this, FROM_INDEX);
  };

  @Field(FROM_INDEX)
  public AbstractEdgeStruct<D> from(long out) {
    this.io.setLongField(this, FROM_INDEX, out);
    return this;
  };

  public abstract D toValue();

  public abstract AbstractEdgeStruct<D> toValue(D toValue);
}