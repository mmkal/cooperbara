/*
 * Copyright (C) 2022 Google Inc.
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

package com.google.cooperbara.onboard.core;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A simple provider that can provide a value for a single {@link Input}.
 *
 * The value is nullable on purpose so that we can return "no value".
 */
public class ConstantProvider<V> implements InputProvider {

  private final Input<V> input;
  @Nullable
  private final V value;
  private final int priority;

  public ConstantProvider(Input<V> input, @Nullable V value) {
    this(input, value, InputProvider.DEFAULT_PRIORITY);
  }
  public ConstantProvider(Input<V> input, @Nullable V value, int priority) {
    this.input = input;
    this.value = value;
    this.priority = priority;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> resolve(Input<T> input, InputProviderResolver db)
      throws InterruptedException, CannotProvideException {
    checkArgument(input == this.input,
        "Requested input %s different of the provided %s."
            + " This shouldn't happen", input, this.input);

    return (Optional<T>) Optional.ofNullable(value);
  }

  @Override
  public ImmutableMap<Input<?>, Integer> provides() throws CannotProvideException {
    return ImmutableMap.of(input, priority);
  }
}
