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

/**
 * Entry point for the library.
 * 
 * @author Christian Trimble
 *
 */
public class Graphene {
  public static class Builder {

    public Graphene build() {
      return new Graphene();
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  Graphene() {

  }
}
