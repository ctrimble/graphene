package com.xiantrimble.graphene.example.commands;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;

/**
 * A value for a chess position.
 * 
 * Results:
 * 
 * 1. WIN - A definitely won position.
 * 2. WINNING - A probably won position, due to overwhelming material.
 * 3. CYCLE - A draw due to cyclic moves.
 * 4. STATEMATE - A draw due to absence of checkmate or legal moves.
 * 5. LOSING - A probably lost position, due to overwhelming material.
 * 6. LOSS - A definitely lost poisition.
 * 7. ILLEGAL - A position that cannot happen due to the rules of chess.  Kings attacking each other, triple check, etc.
 * 8. UNDEFINED - A position that has not yet been evaluated. 
 * 
 * @author Christian Trimble
 *
 */
public class ChessValue extends StructObject {
  public static final int WIN = 7;
  public static final int WINNING = 6;
  public static final int CYCLE = 5;
  public static final int STALEMATE = 4;
  public static final int LOSING = 3;
  public static final int LOSS = 2;
  public static final int ILLEGAL = 1;
  public static final int UNDEFINED = 0;
  
  public static final int RESULT_INDEX = 0;
  public static final int DEPTH_INDEX = 1;

  public ChessValue() {
  }

  public ChessValue(Pointer<? extends StructObject> pointer) {
    super(pointer);
  }

  @Field(RESULT_INDEX)
  public int result() {
    return this.io.getIntField(this, RESULT_INDEX);
  }

  @Field(RESULT_INDEX)
  public ChessValue result(int result) {
    this.io.setIntField(this, RESULT_INDEX, result);
    return this;
  }

  @Field(DEPTH_INDEX)
  public int depth() {
    return this.io.getIntField(this, DEPTH_INDEX);
  }

  @Field(DEPTH_INDEX)
  public ChessValue depth(int depth) {
    this.io.setIntField(this, DEPTH_INDEX, depth);
    return this;
  }
}