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
package com.xiantrimble.graphene.example.commands;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Arguments {

  public static ArgumentAction uniqueSortedList() {
    return new ArgumentAction() {

      @Override
      public boolean consumeArgument() {
        return true;
      }

      @Override
      public void onAttach(Argument argumemt) {
        argumemt.nargs("+");
      }

      @Override
      public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
          Object value) throws ArgumentParserException {
        @SuppressWarnings("unchecked")
        Collection<String> original = (Collection<String>) value;
        List<String> sorted = Lists.newArrayList(Sets.newTreeSet(original));
        if (original.size() != sorted.size()) {
          throw new ArgumentParserException("duplicate elements found", parser, arg);
        }
        attrs.put(arg.getDest(), sorted);
      }

    };
  }

}
