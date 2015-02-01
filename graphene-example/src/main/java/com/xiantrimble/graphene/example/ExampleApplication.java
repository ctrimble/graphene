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
package com.xiantrimble.graphene.example;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import org.reflections.Reflections;

import com.hubspot.dropwizard.guice.GuiceBundle;

import io.dropwizard.Application;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ExampleApplication extends Application<ExampleConfiguration> {

  public static void main(String... args) throws Exception {
    new ExampleApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<ExampleConfiguration> bootstrap) {
    GuiceBundle<ExampleConfiguration> guiceBundle =
        GuiceBundle.<ExampleConfiguration> newBuilder().addModule(new ExampleModule())
            .enableAutoConfig(getClass().getPackage().getName())
            .setConfigClass(ExampleConfiguration.class).build();

    bootstrap.addBundle(guiceBundle);

    Reflections reflections = Reflections.collect();
    reflections.getSubTypesOf(Command.class).forEach(command -> {
      try {
        Constructor<? extends Command> constructor = command.getConstructor();
        bootstrap.addCommand(injected(guiceBundle, constructor.newInstance()));
      } catch (NoSuchMethodException nsme) {
        // move on. Should probably look for annotation, so this can
        // be an error condition.
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(String.format("could not load command type %s",
            command.getName()), e);
      }
    });
  }

  @Override
  public void run(ExampleConfiguration config, Environment env) throws Exception {
    System.out.println("Application GO!");
  }

  public static Command injected(GuiceBundle bundle, Command command) {
    return new InjectedCommand(bundle, command);
  }

  public static class InjectedCommand extends Command {

    private GuiceBundle bundle;
    private Command command;

    protected InjectedCommand(GuiceBundle bundle, Command command) {
      super(command.getName(), command.getDescription());
      this.bundle = bundle;
      this.command = command;
    }

    @Override
    public void configure(Subparser subparser) {
      command.configure(subparser);
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
      bundle.getInjector().injectMembers(command);
      command.run(bootstrap, namespace);
    }

  }
}
