/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a cooper of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cooperbara;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkValue;

/** Starter of feedback migration executions. */
@StarlarkBuiltin(
    name = "trigger",
    doc = "Starter of feedback migration executions.",
    documented = false)
public interface Trigger extends StarlarkValue {

  Endpoint getEndpoint();

  @Override
  default void repr(Printer printer) {
    printer.append(toString());
  }

  ImmutableSetMultimap<String, String> describe();


  /** Returns a key-value list describing the credentials the endpoint was instantiated with. */
  default ImmutableList<ImmutableSetMultimap<String, String>> describeCredentials() {
    return ImmutableList.of();
  }
}
