package com.xiantrimble.graphene;

import static org.junit.Assert.*;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.xiantrimble.graphene.GraphConstructor.Builder;
import com.xiantrimble.graphene.GraphConstructor.EdgePage;
import com.xiantrimble.graphene.junit.GrapheneRule;
import com.xiantrimble.graphene.junit.GrapheneRules;

public class GraphConstructorTest {
  public static @ClassRule GrapheneRule grapheneSupplier = GrapheneRules.graphene();

  GraphConstructor<ChessPartition, ChessEdgeContext, InternalChessEdge> constructor;
  
  @Before
  public void setUp() {

  }
  
  public static GraphConstructor.DefaultBuilder<ChessPartition, ChessEdgeContext, InternalChessEdge> builder() {
  	return GraphConstructor.<ChessPartition, ChessEdgeContext, InternalChessEdge> builder()
		.withGraphene(grapheneSupplier.get())
		.withContextType(ChessEdgeContext.class)
		.withContextSupplier(ChessEdgeContext::new)
		.withEdgeType(InternalChessEdge.class)
		.withPageSize(100000);
  }
  
  @Ignore
  @Test
  public void shouldExpandTwoKings() {
  	// the context we are expanding in.  Should this be here or in the builder?
  	ChessPartition partition = new ChessPartition();
  	
  	constructor = builder()
  			.withPartition(partition)
  			.build();

  			// this should probably be a stream...
  	constructor.stream(new IndexRange().start(0).end(1000000000), 
  			(page)->{
  				System.out.println("page:"+page.getLength());
  				System.out.println("Edge: to:"+page.getData().get(10).to()+" from:"+page.getData().get(10).from());
  			});
  }
  
  public static class ChessPartition extends StructObject {
  	public static final int PIECES_INDEX = 0;
  	public static final long PIECES_SIZE = 64L;
  	public static final int UNPLACED_PIECES_INDEX = 1;
  	public static final long UNPLACED_PIECES_SIZE = 10L;
  	
  	@Array(PIECES_SIZE)
  	@Field(PIECES_INDEX)
  	public Pointer<Integer> pieces() {
  		return this.io.getPointerField(this, PIECES_INDEX);
  	}
  	
  	@Array(PIECES_SIZE)
  	@Field(PIECES_INDEX)
    public ChessPartition pieces( Pointer<Integer> pieces ) {
  		this.io.setPointerField(this, PIECES_INDEX, pieces);
  		return this;
  	}

  	@Array(UNPLACED_PIECES_SIZE)
  	@Field(UNPLACED_PIECES_INDEX)
  	public Pointer<MultisetEntry> unplacedPieces() {
  		return this.io.getPointerField(this, UNPLACED_PIECES_INDEX);
  	}
  	
  	@Array(UNPLACED_PIECES_SIZE)
  	@Field(UNPLACED_PIECES_INDEX)
    public ChessPartition unplacedPieces( Pointer<MultisetEntry> unplacedPieces ) {
  		this.io.setPointerField(this, UNPLACED_PIECES_INDEX, unplacedPieces);
  		return this;
  	}
  }
  
  public static class MultisetEntry extends StructObject {
  	public static final int TYPE_INDEX = 0;
  	public static final int COUNT_INDEX = 1;
  	
  	@Field(TYPE_INDEX)
  	public int type() {
  		return this.io.getIntField(this, TYPE_INDEX);
  	}
  	
  	@Field(TYPE_INDEX)
  	public MultisetEntry type(int type) {
  		this.io.setIntField(this, TYPE_INDEX, type);
  		return this;
  	}
  	
  	@Field(COUNT_INDEX)
  	public int count() {
  		return this.io.getIntField(this, COUNT_INDEX);
  	}
  	
  	@Field(COUNT_INDEX)
  	public MultisetEntry count(int count) {
  		this.io.setIntField(this, COUNT_INDEX, count);
  		return this;
  	}  }
  
  public static class ChessEdgeContext extends EdgeConstructionContext<ChessPartition, ChessEdgeContext> {
    @Field(PARTITION_INDEX)
    public ChessPartition partition() {
      return this.io.getNativeObjectField(this, PARTITION_INDEX);
    };

    @Field(PARTITION_INDEX)
    public ChessEdgeContext partition(ChessPartition partition) {
      this.io.setNativeObjectField(this, PARTITION_INDEX, partition);
      return thisObject();
    }
  	
  }

}
