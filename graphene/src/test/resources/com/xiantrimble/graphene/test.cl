
typedef struct Vertex {
	long data; // this should be user defined.
} Vertex;

typedef struct Edge {
	long to;
	long from;
	long data; // this should be user defined.
} Edge;

typedef struct Context {
	long vertexLength;
	long edgeLength;
	int updates;
} Context;

/*
 * Moves information from the edge nodes to the vertices.
 */
kernel void gather( global Context* context, global Vertex* verticies, global Edge* edges)
{
    int v = get_global_id(0);
    
    if( v < context->vertexLength ) {
      long originalValue = verticies[v].data;
      long value = LONG_MAX;
      for( int e = 0; e < context->edgeLength; e++ ) {
    	  if( edges[e].from == v ) {
    		  if( edges[e].data != LONG_MAX ) {
    			  value = min(edges[e].data + 1, value);
    		  }
    	  }
      }
      barrier(CLK_GLOBAL_MEM_FENCE);
      if( value != LONG_MAX ) {
        verticies[v].data = value;
      
        if( originalValue != value ) {
    	    atomic_add(&context->updates, 1);
        }
      }
    }
    else {
      // hit global memory fence
      barrier(CLK_GLOBAL_MEM_FENCE);
    }
}

/*
 * Moves information from the vertices to the edges.
 */
kernel void scatter( global Context* context, global Vertex* verticies, global Edge* edges )
{
    int e = get_global_id(0);
    
    if( e < context->edgeLength ) {
    	long v = edges[e].to;
    	if( v != LONG_MAX ) {
    		edges[e].data = verticies[v].data;
    	}
    }
}