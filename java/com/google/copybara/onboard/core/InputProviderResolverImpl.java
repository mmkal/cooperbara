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

import static com.google.common.base.Verify.verify;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.cooperbara.onboard.core.AskInputProvider.Mode;
import com.google.cooperbara.util.console.Console;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * This class is in charge of delegating to the proper input provider to resolve Inputs recursively.
 *
 * <p>The {@link InputProvider} uses an internal {@link ImmutableSet<Input>} to detect loops.
 */
public final class InputProviderResolverImpl implements InputProviderResolver {

  private final Converter<Object> starlarkConverter;
  private final Mode askMode;
  private final Console console;
  private final ImmutableSet<String> loopDetector;
  private final Map<Input<?>, InputProvider> inputProviders;

  public static InputProviderResolver create(
      Collection<InputProvider> providers,
      Converter<Object> starlarkConverter,
      Mode askMode,
      Console console)
      throws CannotProvideException {

    HashMultimap<Input<?>, InputProvider> map = HashMultimap.create();
    for (InputProvider provider : providers) {
      for (Input<?> provides : provider.provides().keySet()) {
        map.put(provides, provider);
      }
    }
    Map<Input<?>, InputProvider> providersMap = new HashMap<>();

    for (Entry<Input<?>, Collection<InputProvider>> entry : map.asMap().entrySet()) {
      // Resolver in priority order
      PrioritizedInputProvider provider = new PrioritizedInputProvider(entry.getKey(),
          entry.getValue());
      providersMap.put(
          entry.getKey(),
          // Cache any result
          new CachedInputProvider(
              entry.getKey().inferOnly()
              ? provider
              // Ask user for input depending on the mode
              : new AskInputProvider(provider, askMode, console)));
    }
    return new InputProviderResolverImpl(
        providersMap,
        starlarkConverter,
        askMode,
        console,
        ImmutableSet.of());
  }

  private InputProviderResolverImpl(
      Map<Input<?>, InputProvider> inputProviders,
      Converter<Object> starlarkConverter,
      Mode askMode,
      Console console,
      ImmutableSet<String> loopDetector) {
    this.starlarkConverter = starlarkConverter;
    this.askMode = askMode;
    this.console = console;
    this.loopDetector = loopDetector;
    this.inputProviders = inputProviders;
  }

  /** Resolve the value for an {@link Input} object. */
  @Override
  public <T> T resolve(Input<T> input)
      throws InterruptedException, CannotProvideException {
    if (loopDetector.contains(input.name())) {
      throw new IllegalStateException(
          "Loop detected trying to resolver input: "
              + Joiner.on(" -> ").join(loopDetector)
              + " -> *"
              + input.name());
    }
    InputProvider inputProvider = inputProviders.get(input);
    // Register an on-demand provider for an Input that is not provided by any InputProvider.
    // This is going to mean that we ask the user for the value (if the mode allows it).
    if (inputProvider == null) {
      CachedInputProvider newProvider =
          new CachedInputProvider(
              new AskInputProvider(new ConstantProvider<>(input, null), askMode, console));
      inputProviders.put(input, newProvider);
      return resolveAndCheck(input, newProvider, this);
    }

    verify(
        inputProvider.provides().containsKey(input),
        "Something went wrong, InputProvider %s doesn't provide %s",
        inputProvider,
        input);

    return resolveAndCheck(
        input,
        inputProvider,
        new InputProviderResolverImpl(
            inputProviders,
            starlarkConverter,
            askMode,
            console,
            ImmutableSet.<String>builder().addAll(loopDetector).add(input.name()).build()));
  }

  private <T> T resolveAndCheck(
      Input<T> input, InputProvider provider, InputProviderResolver inputProviderResolver)
      throws InterruptedException, CannotProvideException {
    Optional<T> result = provider.resolve(input, inputProviderResolver);
    if (result.isEmpty()) {
      throw new CannotProvideException(String.format(
          "Cannot find a value for '%s' (%s)", input.description(), input.name()));
    }
    try {
      return input.type().cast(result.get());
    } catch (ClassCastException unused) {
      throw new IllegalStateException(
          String.format(
              "Input provider %s returned an object of type %s, but %s requires an object"
                  + " of type %s",
              provider, result.get().getClass(), input, input.type()));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T parseStarlark(String starlark, Class<T> type) throws CannotConvertException {
    Object convert = starlarkConverter.convert(starlark, this);
    if (!type.isAssignableFrom(convert.getClass())) {
      throw new CannotConvertException(
          String.format("Invalid input: %s. Not of type %s", starlark, type.getName()));
    }
    return (T) convert;
  }
}
