

\#include "com/xiantrimble/graphene/factorial.cl"

\#ifdef PERM_
//
// Context specific definitions.
//
#ifndef CHESS_UNIQUE_UNPLACED_PIECES
#define CHESS_UNIQUE_UNPLACED_PIECES 2
#endif

// undef permutation macro variables.
#undef PERM_UNIQUE_ELEMENTS
#undef PERM_ELEMENT_TYPE

// define the indice permutation functions.
#define PERM_UNIQUE_ELEMENTS CHESS_UNIQUE_UNPLACED_PIECES
#include "com/xiantrimble/graphene/multiset_permutation_indices.cl"

// define the element functions
#define PERM_ELEMENT_TYPE int
#include "com/xiantrimble/graphene/multiset_permutation_elements.cl"

#ifndef vertex_partition_struct
typedef struct {
  int pieces[64];
  PERM_MULTISET_ELEMENT_STRUCT(PERM_ELEMENT_TYPE) unplacedPieces[10];
} ChessPartition;
#define vertex_partition_struct ChessPartition
#endif

// cleanup permutation macro variables.
#undef PERM_UNIQUE_ELEMENTS
#undef PERM_ELEMENT_TYPE

//
// Default Vertex Partition.
//
#ifndef vertex_partition_struct
typedef struct {
} DefaultVertexPartition;
#define vertex_partition_struct DefaultVertexPartition
#endif

/*
 * The context for edge construction. 
 */
typedef struct {
	IndexRange range;
	vertex_partition_struct partition;
	int edgeIndex;
	int edgesLength;
	int overflowIndex;
} EdgeConstructionContext;

void addEdge( global EdgeConstructionContext* context, uint rangeOffset, global Edge* edges, Edge edge );

/**
 * This definition is lower than I would like it.  Looks like the structs and function declarations need
 * to be broken off into a seperate file.
 */
#ifndef visit_next_vertices
void default_visit_next_vertices(global EdgeConstructionContext* context, long rangeOffset, global Edge* edges );
void default_visit_next_vertices(global EdgeConstructionContext* context, long rangeOffset, global Edge* edges ) {
	ChessPartition partition = context->partition;
	
	// get the index of the position.
	long index = rangeOffset + context->range.start;
	
	// take half of the index, that is the permutation we are on.
	long permutationIndex = index / 2L;
	
	// mod 2 the index, this is the turn.  0 = White, 1 = Black.
	int turn = index % 2;
	
	// expand the piece permutation for the partition.
	int pieces[CHESS_UNIQUE_UNPLACED_PIECES];
	//PERM_EXPAND_ELEMENTS(CHESS_UNIQUE_UNPLACED_PIECES)(partition.unplacedPieces, )
	
	
	// blend the piece permutation with the board.
	
	// start expanding positions.
	// use the range offset to build the c
	Edge edge = {.to = rangeOffset, .from = rangeOffset};
	addEdge(context, rangeOffset, edges, edge);
}
#define visit_next_vertices(context, rangeOffset, edges) default_visit_next_vertices(context, rangeOffset, edges)
#endif

/*
 * Users must define visit_next_vertices.
 */
#ifndef visit_next_vertices
void default_visit_next_vertices(global EdgeConstructionContext* context, long rangeOffset, global Edge* edges );
void default_visit_next_vertices(global EdgeConstructionContext* context, long rangeOffset, global Edge* edges ) {
	Edge edge = {.to = rangeOffset, .from = rangeOffset};
	addEdge(context, rangeOffset, edges, edge);
}
#define visit_next_vertices(context, rangeOffset, edges) default_visit_next_vertices(context, rangeOffset, edges)
#endif

kernel void constructEdges( global EdgeConstructionContext* context, global Edge* edges ) {
    int rangeOffset = get_global_id(0);
    
    // test to see if we are in range.
    if( rangeOffset + context->range.start < context->range.end && context->overflowIndex < 0 ) {
        $visitNextVertices( context, rangeOffset, edges );
    }
}
void addEdge( global EdgeConstructionContext* context, uint rangeOffset, global Edge* edges, Edge edge ) {
	int edgeIndex = atomic_add(&context->edgeIndex, 1);
	
	// if we have room, then add the edge.
	if( edgeIndex < context->edgesLength ) {
		edges[edgeIndex] = edge;
	}
	// otherwise, record the index that overflowed.
	else {
		atomic_min(&context->overflowIndex, rangeOffset); 
	}
}
