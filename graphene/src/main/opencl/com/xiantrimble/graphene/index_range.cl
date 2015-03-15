
/*
 * Type definition for a range.  The start index is inclusive and the
 * end index is exclusive.  A range where the start and end are equal is the
 * empty range.
 */
#ifndef INDEX_RANGE_STRUCT
#define INDEX_RANGE_STRUCT
typedef struct {
	// inclusive start index.
	long start;
	// exclusive end index.
	long end;
} IndexRange;
#endif