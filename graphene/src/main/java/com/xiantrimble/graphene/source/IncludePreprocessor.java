package com.xiantrimble.graphene.source;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Manages the loading and preprocessing of source files.
 * 
 * @author Christian Trimble
 */
public class IncludePreprocessor {
  static Pattern includePattern;
  static {
  	try {
  		includePattern = Pattern.compile("^\\s*#include\\s+\"([^\"]*)\"\\s*$");
  	} catch( PatternSyntaxException pse ) {
  		// TODO: this should be logged.
  		pse.printStackTrace();
  	}
  }
  
  public static class Builder {
  	Resolver<Source> resolver;
  	
  	public Builder withResolver( Resolver<Source> resolver ) {
  		this.resolver = resolver;
  		return this;
  	}
  	
  	public IncludePreprocessor build() {
  		return new IncludePreprocessor(resolver);
  	}
  }
  
  public static Builder builder() {
  	return new Builder();
  }
  
  Resolver<Source> resolver;

  public IncludePreprocessor(Resolver<Source> resolver) {
	  this.resolver = resolver;
  }

	public Source preprocess( Source source ) {
  	String[] lines = source.getSource().split("(?:\\r\\n|\\r|\\n)");
  	
  	StringBuilder processed = new StringBuilder();
  	
  	processed.append("#line 1 \"").append(source.getSystemId()).append("\"\n");
  	for( int i = 0; i < lines.length; i++ ) {
  		Matcher matcher = includePattern.matcher(lines[i]);
  		if( matcher.matches() ) {
  			String includeHref = matcher.group(1);
  			Source includeSource = resolver.resolve(includeHref, source.getSystemId());
  			if( includeSource == null ) {
  				// TODO: better exception.
  				throw new RuntimeException("could not resolve include.");
  			}
  			includeSource = preprocess(includeSource);
  			processed.append(includeSource.getSource());
  			if( !endsWithNewLine(includeSource.getSource())) {
  				processed.append("\n");
  			}
  			processed.append("#line ").append(i+2).append(" \"").append(source.getSystemId()).append("\"\n");
  		}
  		else {
  			processed.append(lines[i]).append("\n");
  		}
  	}
  	return new Source()
  	  .withSystemId(source.getSystemId())
  	  .withSource(processed.toString());
  }
  
  public static boolean endsWithNewLine( String value ) {
  	return value.endsWith("\r\n") ||
  			value.endsWith("\r") ||
  			value.endsWith("\n");
  }
}
