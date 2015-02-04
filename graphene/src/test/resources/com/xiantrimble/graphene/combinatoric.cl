// general combinatoric functions.
long binomialCoefficient( int n, int k );
long binomialCoefficient( int n, int k ) {
	long result = 1;
	for( int i = 1; i <= k; i++ ) {
		result = (result * (n + 1 - i) / i);
	}
	return result;
}

typedef struct {
    int nOffset; // size of the problem
    int kOffset; // number of unique elements
} BinomialCoefficientContext;

kernel void binomial_coefficient_table( global BinomialCoefficientContext* context, global long* results)
{
  int i = get_global_id(0);
  int j = get_global_id(1);
  int jSize = get_global_size(1);
  
  int n = context->nOffset + (long)i;
  int k = context->kOffset + (long)j;
  long index = ((long)i * (long)jSize) + (long)j;
  
  results[index] = binomialCoefficient(n, k);
}

constant static long FACTORIALS[21] = { 
  1l, 1l, 2l, 6l, 24l, 120l, 720l, 5040l, 40320l, 362880l,
  3628800l, 39916800l, 479001600l, 6227020800l, 87178291200l,
  1307674368000l, 20922789888000l, 355687428096000l,
  6402373705728000l, 121645100408832000l, 2432902008176640000l };

/*
long multisetPermutationCount( int* m, int mSize );
long multisetPermutationCount( int* m, int mSize ) {
	// weakest implementation ever.
	int n = 0;
	int d = 1;
	for( int i = 0; i < mSize; i++ ) {
		n += m[i];
		d *= m[i];
	}
	FACTORIALS[n] / d;
}
*/

//
// Kernels for converting combinations and permutations, with and without duplicates,
// from their indices to their element representations and back again.
//
// Element representations are stored in int arrays, with 4 elements stored in
// each int.  Up to 256 unique elements are supported.  The length of the combinations
// and permutations are up to 32 elements in length (array of 8 32-bit integers).
//
// Suffixes:
// nr - no repetition
// lr - limited repetition
// ur - unlimited repetition

typedef struct {
    int size; // size of the problem
    int uniqueElements; // number of unique elements
    int subsetSize; // number of elements in each result.
} CombinatoricContext;

/*
 * Expands the specified combination indices to their element forms.
 * 
 * context - information about the combinatoric being expanded.
 * indices - an array of indices to expand.
 * elements - the output of the expansion.
 */
kernel void expand_comb_nr( global CombinatoricContext* context, global long* indices, global int* elements)
{
	int i = get_global_id(0);
	
	if( i < context->size ) {
	  long index = indices[i];
	  int uniqueElements = context->uniqueElements;
	  int n = uniqueElements;
	  int k = context->subsetSize;
	  for( int e = 0; e < context->subsetSize; e++ ) { // e is index in output array
	    long boundary = 0;
	    while( (boundary = binomialCoefficient(n-1, k-1)) <= index ) {
	    	index -= boundary;
	    	n--;
	    }
	    elements[(i*context->subsetSize)+e] = uniqueElements-n;
	    k--;
	    n--;
	  }
	}
}


kernel void index_comb_nr( global CombinatoricContext* context, global long* indices, global int* elements ) {
	int i = get_global_id(0);
	
	if( i < context->size ) {
		  int uniqueElements = context->uniqueElements;
		  int n = uniqueElements;
		  int k = context->subsetSize;
		  long index = 0;
		  for( int e = 0; e < context->subsetSize; e++ ) {
			  int element = uniqueElements - elements[(i*context->subsetSize)+e];
			  while( n != element ) {
				  index += binomialCoefficient(n-1, k-1);
				  n--;
			  }
			  n--;
			  k--;
		  }
		  indices[i] = index;
	}
}