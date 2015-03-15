package com.xiantrimble.graphene;

import org.bridj.Pointer;
import org.bridj.StructObject;

public abstract class AbstractVertexStruct<D extends StructObject> extends StructObject {
  public static final int VALUE_INDEX = 0;
  public static final int INITIAL_VALUE_INDEX = 1;

  public AbstractVertexStruct() {
  }

  public AbstractVertexStruct(Pointer<? extends StructObject> pointer) {
    super(pointer);
  }

  public abstract D value();

  public abstract AbstractVertexStruct<D> value(D data);

  public abstract D initialValue();

  public abstract AbstractVertexStruct<D> initialValue(D initialValue);
}