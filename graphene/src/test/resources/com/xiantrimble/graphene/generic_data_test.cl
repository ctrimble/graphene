
/*
 * graphene_value_type defines the name of the vertex value structure.
 */
#ifndef graphene_value_type
#define graphene_value_type GrapheneDefaultValue
typedef struct {
	int distance;
} GrapheneDefaultValue;
#endif

/*
 * graphene_previous_value is a function that returns a to value translated into a from value.
 */
#ifndef graphene_previous_value
GrapheneDefaultValue defaultPreviousValue(GrapheneDefaultValue value);
GrapheneDefaultValue defaultPreviousValue(GrapheneDefaultValue value) {
	GrapheneDefaultValue result = {value.distance+1};
	return result;
}
#define graphene_previous_value(x) defaultPreviousValue(x)
#endif

#ifndef graphene_compare_values
int defaultCompareValues(GrapheneDefaultValue x, GrapheneDefaultValue y);
int defaultCompareValues(GrapheneDefaultValue x, GrapheneDefaultValue y) {
	return x.distance==y.distance?0:x.distance>y.distance?1:-1;
}
#define graphene_compare_values(x,y) defaultCompareValues(x, y)
//#define graphene_compare_values(x,y) (x.distance==y.distance?0:x.distance>y.distance?1:-1)
#endif

typedef struct {
	graphene_value_type value;
	graphene_value_type initialValue;
} Vertex;

typedef struct {
	long to;
	long from;
	graphene_value_type toValue;
} Edge;

typedef struct {
	long vertexOffset;
	int vertexLength;
	int edgeLength;
} DataContext;

typedef struct {
	int updates;
} ComputeContext;

/*
 * Moves information from the edge nodes to the vertices.
 */
kernel void gather( global ComputeContext* computeContext, global const DataContext* dataContext, global Vertex* verticies, global Edge* edges)
{
    int v = get_global_id(0);
    
    if( v < dataContext->vertexLength ) {
      graphene_value_type value = verticies[v].initialValue;
      for( int e = 0; e < dataContext->edgeLength; e++ ) {
    	  if( edges[e].from == dataContext->vertexOffset + v ) {
    		  graphene_value_type toValue = edges[e].toValue;
    		  graphene_value_type fromValue = graphene_previous_value(toValue);
    		  int compare = graphene_compare_values(value, fromValue);
    		  value = compare < 0 ? value : fromValue;
    	  }
      }
      
      // do I need this?
      barrier(CLK_GLOBAL_MEM_FENCE);
      
      graphene_value_type vertexValue = verticies[v].value;
      if( graphene_compare_values(vertexValue, value) != 0 ) {
        atomic_add(&computeContext->updates, 1);
      }
      
      verticies[v].value = value;      
    }
    else {
      // hit global memory fence
      barrier(CLK_GLOBAL_MEM_FENCE);
    }
}

/*
 * Moves information from the vertices to the edges.
 */
kernel void scatter( global ComputeContext* computeContext, global const DataContext* dataContext, global Vertex* verticies, global Edge* edges )
{
    int e = get_global_id(0);
    
    if( e < dataContext->edgeLength ) {
    	if( edges[e].to >= dataContext->vertexOffset && edges[e].to < dataContext->vertexOffset + dataContext->vertexLength) {
    		edges[e].toValue = verticies[edges[e].to-dataContext->vertexOffset].value;
    	}
    }
}