#ifndef PERM_UNIQUE_ELEMENTS
#define PERM_UNIQUE_ELEMENTS 6
#endif

#include "com/xiantrimble/graphene/factorial.h"
#include "com/xiantrimble/graphene/multiset_permutation_indices.cl"

typedef struct {
    int size; // size of the problem
    int uniqueElements; // number of unique elements
} PermutationSizeContext;

kernel void compute_permutation_sizes( global PermutationSizeContext* context, global int* multisets, global long* sizes ) {
	int i = get_global_id(0);
	
	if( i < context->size ) {
		int multiset[PERM_UNIQUE_ELEMENTS];
		PERM_COPY_DOMAIN_FROM_GLOBAL(PERM_UNIQUE_ELEMENTS)(multisets+(i * PERM_UNIQUE_ELEMENTS), multiset);
		sizes[i] = PERM_SIZE(PERM_UNIQUE_ELEMENTS)(multiset);
	}
}

typedef struct {
    int size; // size of the problem
    int uniqueElements; // number of unique elements
    int subsetSize; // number of elements in each result.
} CombinatoricContext;

/*
 * Expands the specified permutation indices to their element forms.
 * 
 * context - information about the combinatoric being expanded.
 * indices - an array of indices to expand.
 * elements - the output of the expansion.
 */

kernel void expand_perm_lr( global CombinatoricContext* context, global int* domain, global long* indices, global int* elements )
{
	int i = get_global_id(0);
	
	if( i < context->size ) {
		int multiset[PERM_UNIQUE_ELEMENTS];
		PERM_COPY_DOMAIN_FROM_GLOBAL(PERM_UNIQUE_ELEMENTS)(domain, multiset);
		PERM_EXPAND_INDICES(PERM_UNIQUE_ELEMENTS)(multiset, indices[i], elements+(i*context->subsetSize), context->subsetSize);
	}
}

kernel void index_perm_lr( global CombinatoricContext* context, global int* domain, global long* indices, global int* elements )
{
	int i = get_global_id(0);
	
	if( i < context->size ) {
		int multiset[PERM_UNIQUE_ELEMENTS];
		PERM_COPY_DOMAIN_FROM_GLOBAL(PERM_UNIQUE_ELEMENTS)(domain, multiset);
		indices[i] = PERM_INDEX_INDICES(PERM_UNIQUE_ELEMENTS)(multiset, elements+(i*context->subsetSize), context->subsetSize);
	}
}