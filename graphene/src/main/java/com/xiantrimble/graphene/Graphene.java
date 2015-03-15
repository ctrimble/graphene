/**
 * Copyright (C) 2014 Christian Trimble (xiantrimble@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xiantrimble.graphene;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bridj.Pointer;

import com.google.common.collect.Lists;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.dsl.Disruptor;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.xiantrimble.graphene.source.Resolver;
import com.xiantrimble.graphene.source.Source;

import static com.xiantrimble.graphene.JavaCLUtils.releaseQuietly;
import static com.xiantrimble.graphene.JavaCLUtils.devices;

/**
 * Entry point for the library.
 * 
 * @author Christian Trimble
 *
 */
public class Graphene implements Closeable {
  public static class Builder {
  	
  	ExecutorService executor;
		private Resolver<Source> resolver;
  	
  	public Builder withExecutor(ExecutorService executor) {
  		this.executor = executor;
  		return this;
  	}
  	
  	public Builder withIncludeResolver( Resolver<Source> resolver ) {
  		this.resolver = resolver;
  		return this;
  	}

    public Graphene build() {
    	
    	boolean manageExecutor = executor == null;
    	if( manageExecutor ) {
    		this.executor = Executors.newCachedThreadPool();
    	}
    	
    	List<GrapheneDeviceContext> contexts = devices()
    			.stream()
    			.map(GrapheneDeviceContext::new)
    			.collect(Collectors.toList());

      return new Graphene(contexts, executor, manageExecutor);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
  
	protected List<GrapheneDeviceContext> contexts;
	protected ExecutorService executor;
	protected boolean manageExecutor;

  Graphene(List<GrapheneDeviceContext> contexts, ExecutorService executor, boolean manageExecutor) {
    this.contexts = contexts;
    this.executor = executor;
    this.manageExecutor = manageExecutor;
  }
  
  public List<GrapheneDeviceContext> getContexts() {
  	return contexts;
  }
  
  public Executor getExecutor() {
  	return this.executor;
  }
  
  public ComputationContext createComputationContext() {
  	return new ComputationContext();
  }

	@Override
  public void close() {
	  contexts.forEach(GrapheneDeviceContext::close);
	  // Notes: perhaps this entire block should plugin.
	  if( manageExecutor ) {
	  	try {
	  		executor.shutdown();
	  		executor.awaitTermination(1, TimeUnit.MINUTES);
	  	}
	  	catch( InterruptedException ie ) {
	  		// TODO: log this problem.
	  	}
	  }
  }
	
	public class ComputationContext implements Closeable {
		
		List<Disruptor<?>> disruptors = Lists.newArrayList();
		List<DeviceComputationContext> deviceComputationContexts;
		
		public ComputationContext() {
			deviceComputationContexts = getContexts()
					.stream()
					.map(DeviceComputationContext::new)
					.collect(Collectors.toList());
		}
		
		public List<DeviceComputationContext> getDeviceComputationContexts() {
			return deviceComputationContexts;
		}

		public <T> Disruptor<T> createDisruptor(EventFactory<T> eventFactory, int ringBufferSize) {
	    Disruptor<T> disruptor = new Disruptor<T>(eventFactory, ringBufferSize, getExecutor());
	    disruptors.add(disruptor);
	    return disruptor;
    }
		
		@Override
    public void close() {
	    disruptors.forEach(DisruptorUtils::shutdownDisruptor);
    }
		
		public class DeviceComputationContext implements Closeable {
			GrapheneDeviceContext deviceContext;
			List<CLProgram> programs = Lists.newArrayList();
			List<AutoCloseable> closeables = Lists.newArrayList();
			
			public DeviceComputationContext( GrapheneDeviceContext deviceContext ) {
				this.deviceContext = deviceContext;
			}
			
			public GrapheneDeviceContext getDeviceContext() {
				return this.deviceContext;
			}
			
			public CLProgram createProgram( String... sources ) {
				return registerCloseable(deviceContext.getContext().createProgram(sources));
			}
			
			public <T> CLBuffer<T> createBuffer(Usage kind, Pointer<T> data) {
			  return registerCloseable(deviceContext.getContext().createBuffer(kind, data));
			}
			
			protected <T> CLBuffer<T> registerCloseable(CLBuffer<T> buffer) {
				closeables.add(()->releaseQuietly(buffer));
				return buffer;
			}

			protected CLProgram registerCloseable(CLProgram program) {
				closeables.add(()->releaseQuietly(program));
				return program;
			}

	    @Override
      public void close() {
				closeables.forEach(Graphene::closeQuietly);
      }
			
		}
	}
  
  public static class GrapheneDeviceContext implements Closeable {
  	GrapheneDeviceContext( CLDevice device, CLContext context, CLQueue queue, ByteOrder byteOrder ) {
  		this.device = device;
  		this.context = context;
  		this.queue = queue;
  		this.byteOrder = byteOrder;
  	}
  	GrapheneDeviceContext( CLDevice device ) {
  		this.device = device;
  		this.context = JavaCLUtils.createContext(device);
  		this.queue = context.createDefaultQueue();
  		this.byteOrder = device.getByteOrder();
  	}
		protected CLDevice device;
  	protected CLContext context;
  	protected CLQueue queue;
  	protected ByteOrder byteOrder;
  	
  	public CLDevice getDevice() {
			return device;
		}
		public CLContext getContext() {
			return context;
		}
		public CLQueue getQueue() {
			return queue;
		}
		public ByteOrder getByteOrder() {
			return byteOrder;
		}


		@Override
    public void close() {
	    releaseQuietly(queue);
	    releaseQuietly(context);
	    releaseQuietly(device);
    }
  }
  
  public static void closeQuietly( AutoCloseable closeable ) {
  	if( closeable != null ) {
  		try {
  			closeable.close();
  		}
  		catch( Exception e ) {
  			// TODO: log this.
  		}
  	}
  }
}
