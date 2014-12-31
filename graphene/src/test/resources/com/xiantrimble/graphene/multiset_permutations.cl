constant static long FACTORIALS[21] = { 
  1l, 1l, 2l, 6l, 24l, 120l, 720l, 5040l, 40320l, 362880l,
  3628800l, 39916800l, 479001600l, 6227020800l, 87178291200l,
  1307674368000l, 20922789888000l, 355687428096000l,
  6402373705728000l, 121645100408832000l, 2432902008176640000l };

long factorial( long n );
long factorial( long n ) {
	if( n > 21 ) return LONG_MAX;
	return FACTORIALS[n];
}

#ifndef PERM_UNIQUE_ELEMENTS
#define PERM_UNIQUE_ELEMENTS 6
#endif

long multiset_permutations( int m[static PERM_UNIQUE_ELEMENTS], int offset, int length );
long multiset_permutations( int m[static PERM_UNIQUE_ELEMENTS], int offset, int length ) {
	long total = 0;
	long denum = 1;
	for( int i = 0; i < length; i++ ) {
		int count = m[i+offset];
		total += count;
		denum *= count == 0 ? 1 : factorial(count);
	}
	return factorial(total)/denum;
}

typedef struct {
    int size; // size of the problem
    int uniqueElements; // number of unique elements
} PermutationSizeContext;

kernel void compute_permutation_sizes( global PermutationSizeContext* context, global int* multisets, global long* sizes ) {
	int i = get_global_id(0);
	
	if( i < context->size ) {
		
	    long offset = i * PERM_UNIQUE_ELEMENTS;
		int multiset[PERM_UNIQUE_ELEMENTS];
		for( int m = 0; m < PERM_UNIQUE_ELEMENTS; m++ ) {
	      multiset[m] = multisets[offset+m];
		}
		
		sizes[i] = multiset_permutations(multiset, 0, PERM_UNIQUE_ELEMENTS);
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
		for( int m = 0; m < PERM_UNIQUE_ELEMENTS; m++ ) {
			multiset[m] = domain[m];
		}
		
		long index = indices[i];
		
		// for each element...
		for( int e = 0; e < context->subsetSize; e++ ) {
			
			// iterate over the multiset, testing the end of it's 
			// bounds is past the index.
			for( int m = 0; m < PERM_UNIQUE_ELEMENTS; m++ ) {
			  int remaining = multiset[m];
			  if( remaining == 0 ) continue;
				  multiset[m]--;
				  long size = multiset_permutations(multiset, 0, PERM_UNIQUE_ELEMENTS);
				  if( size > index ) {
					  elements[(i*context->subsetSize)+e] = m;
					  break;
				  }
				  else {
					multiset[m]++;
			        index-=size;
				  }
			  }
		}
	}
}

kernel void index_perm_lr( global CombinatoricContext* context, global int* domain, global long* indices, global int* elements )
{
	int i = get_global_id(0);
	
	if( i < context->size ) {
		int multiset[PERM_UNIQUE_ELEMENTS];
		for( int m = 0; m < PERM_UNIQUE_ELEMENTS; m++ ) {
			multiset[m] = domain[m];
		}
		
		int index = 0;
		
		// for each element...
		for( int e = 0; e < context->subsetSize; e++ ) {
			int element = elements[(i*context->subsetSize)+e];
			
			// iterate over the multiset, testing the end of it's 
			// bounds is past the index.
			for( int m = 0; m < PERM_UNIQUE_ELEMENTS; m++ ) {
				int remaining = multiset[m];
				if( remaining == 0 ) continue;
				multiset[m]--;
				if( m < element ) {
					index += multiset_permutations(multiset, 0, PERM_UNIQUE_ELEMENTS);
					multiset[m]++;
				}
				else {
					break;
				}
			  }
		}
		indices[i] = index;
	}
}