/*
 * Structures for describing dense graphs.
 */

/*
 * A macro and default implementation for the value attached to a graph vertex.
 */
#ifndef graphene_value_type
#define graphene_value_type GrapheneDefaultValue
typedef struct {
	int distance;
} GrapheneDefaultValue;
#endif

/*
 * Structure for describing a dense vertex, vertices are "dense" when their
 * ID can easily tied directly to their index, without producing a large amount
 * of waste.
 * 
 * Note: This vertex has both the initial value and the current value, used in
 * iterative problem solving.  This may be improved be seperating these into
 * two distinct arrays of values.
 */
typedef struct {
	graphene_value_type value;
	graphene_value_type initialValue;
} DenseVertex;

/*
 * Structure for describing an edge.
 * 
 * This representation contains a to value, used in iterative problem solving.
 */
typedef struct {
	long to;
	long from;
	graphene_value_type toValue;
} Edge;