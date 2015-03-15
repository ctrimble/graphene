package com.xiantrimble.graphene;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class Utils {

	/**
		 * Returns an iterable that is backed by a supplier.  A null value indicates end of iteration.
		 * @param s
		 * @return
		 */
	public static <T> Iterable<T> iterable( Supplier<T> s ) {
	  return  () -> {
		return new Iterator<T>() {
			T next;
			boolean hasNext = true;
			@Override
	    public boolean hasNext() {
				if( !hasNext ) return false;
				if( next != null ) return true;
				hasNext = (next = s.get()) != null ? true : false;
				return hasNext();
	    }
	
			@Override
	    public T next() {
				if( !hasNext()) {
					throw new NoSuchElementException();
				}
				T result = next;
				next = null;
				return result;
	    }
		};
	  };
	}

}
