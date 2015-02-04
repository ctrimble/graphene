constant static const int WIN = 4;
constant static const int STALEMATE = 2;
constant static const int CYCLE = 3;
constant static const int LOSS = 1;
constant static const int UNDEFINED = 0;

#define ex_compare_int(x,y) x > y ? 1 : x == y ? 0 : -1

#define graphene_value_type ComplexValue
typedef struct {
	int result;
	int depth;
} ComplexValue;

#define graphene_previous_value(x) ex_previous_value(x)
ComplexValue ex_previous_value( ComplexValue value );
ComplexValue ex_previous_value( ComplexValue value ) {
	ComplexValue previous = {UNDEFINED, 0};
	switch( value.result ) {
		case WIN:
			previous.result = LOSS;
			previous.depth = value.depth+1;
			break;
		case STALEMATE:
			previous.result = STALEMATE;
			previous.depth = value.depth+1;
			break;
		case CYCLE:
			previous.result = CYCLE;
			previous.depth = value.depth;
			break;
		case LOSS:
			previous.result = WIN;
			previous.depth = value.depth+1;
			break;
	}
	return previous;
}

#define graphene_compare_values(x,y) ex_compare_values(x, y)
int ex_compare_values( ComplexValue a, ComplexValue b );
int ex_compare_values( ComplexValue a, ComplexValue b ) {
	int result = ex_compare_int(a.result, b.result);
	if( result == 0 ) {
	  switch(a.result) {
		case WIN:
		  result = ex_compare_int(b.depth, a.depth);
		  break;
		case STALEMATE:
			result = ex_compare_int(a.depth, b.depth);
          break;
		case CYCLE:
			result = ex_compare_int(a.depth, b.depth);
			break;
		case LOSS:
			result = ex_compare_int(a.depth, b.depth);
			break;
	    case UNDEFINED:
	    	result = ex_compare_int(a.depth, b.depth);
			break;
	    default:
	    	result = 0;
	  }
	}
	return - result;
}



