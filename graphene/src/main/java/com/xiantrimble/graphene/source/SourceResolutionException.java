package com.xiantrimble.graphene.source;

public class SourceResolutionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SourceResolutionException() {
	  super();
  }

	public SourceResolutionException(String message, Throwable cause,
      boolean enableSuppression, boolean writableStackTrace) {
	  super(message, cause, enableSuppression, writableStackTrace);
  }

	public SourceResolutionException(String message, Throwable cause) {
	  super(message, cause);
  }

	public SourceResolutionException(String message) {
	  super(message);
  }

	public SourceResolutionException(Throwable cause) {
	  super(cause);
  }

}
