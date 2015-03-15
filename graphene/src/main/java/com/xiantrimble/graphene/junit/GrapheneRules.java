package com.xiantrimble.graphene.junit;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.xiantrimble.graphene.Graphene;

public class GrapheneRules {
	
	/**
	 * A rule that provides a Graphene context, if OpenCL is available.  Otherwise,
	 * the test using the rule is skipped.
	 * 
	 * @return
	 */
  public static GrapheneRule graphene() {
  	return new GrapheneRule() {
  		
  		private Graphene graphene;

			@Override
      public Statement apply(final Statement base, Description description) {
		    return new Statement() {
		      @Override
		      public void evaluate() throws Throwable {
		      	OpenCLRules.assertOpenCLAvailable();
			      graphene = Graphene.builder().build();
			      try {
			      	base.evaluate();
			      } finally {
			      	graphene.close();
			      	graphene = null;
			      }
		      }
		    };

      }

			@Override
      public Graphene get() {
	      return graphene;
      }
  		
  	};
  }
}
