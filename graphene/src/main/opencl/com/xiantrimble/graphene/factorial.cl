#include "./factorial.h"

/*
 * The first 21 factorials, starting at 0.
 */
constant static long FACTORIALS[21] = { 
  1l, 1l, 2l, 6l, 24l, 120l, 720l, 5040l, 40320l, 362880l,
  3628800l, 39916800l, 479001600l, 6227020800l, 87178291200l,
  1307674368000l, 20922789888000l, 355687428096000l,
  6402373705728000l, 121645100408832000l, 2432902008176640000l };

/*
 * Returns the factorial of n, or LONG_MAX if n > 21.
 */
long factorial( long n ) {
	if( n > 21 ) return LONG_MAX;
	return FACTORIALS[n];
}