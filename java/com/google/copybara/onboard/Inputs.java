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

package com.google.cooperbara.onboard;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.cooperbara.authoring.Author;
import com.google.cooperbara.authoring.AuthorParser;
import com.google.cooperbara.authoring.InvalidAuthorException;
import com.google.cooperbara.configgen.ConfigGenHeuristics.GeneratorTransformations;
import com.google.cooperbara.onboard.core.CannotConvertException;
import com.google.cooperbara.onboard.core.Converter;
import com.google.cooperbara.onboard.core.Input;
import com.google.cooperbara.onboard.core.InputProviderResolver;
import com.google.cooperbara.onboard.core.template.ConfigGenerator;
import com.google.cooperbara.util.Glob;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Standard {@link Input}s that can be used by config generators.
 */
public class Inputs {

  private Inputs() {}

  private static final Converter<URL> URL_CONVERTER =
      (s, resolver) -> {
        try {
          return new URL(s);
        } catch (MalformedURLException e) {
          throw new CannotConvertException("Invalid url " + s + ": " + e);
        }
      };
  public static final Input<URL> GIT_ORIGIN_URL = Input.create(
      "git_origin_url", "Git URL to serve as origin repository",
      null, URL.class, URL_CONVERTER);

  public static final Input<String> CURRENT_VERSION =
      Input.create(
          "current_version",
          "Current imported version or version wanted",
          null,
          String.class,
          (value, resolver) -> value);

  public static final Input<URL> GIT_DESTINATION_URL = Input.create(
      "git_destination_url", "Git URL to serve as origin repository",
      null, URL.class, URL_CONVERTER);

  /** Should be accessed as optional. As it can only be inferred */
  public static final Input<GeneratorTransformations> TRANSFORMATIONS = Input.createInfer(
      "transformations", "`core.move`s and other transformations",
      null, GeneratorTransformations.class);

  public static final Input<Boolean> NEW_PACKAGE =
      Input.createInfer(
          "new_package",
          "Whether or not this package already exists in third_party",
          null,
          Boolean.class);

  public static final Input<Glob> ORIGIN_GLOB =
      Input.create(
          "origin_glob",
          "Glob of files to be migrated from the origin",
          Glob.ALL_FILES,
          Glob.class,
          new Converter<Glob>() {
            @Override
            public Glob convert(String value, InputProviderResolver resolver)
                throws CannotConvertException {
              try {
                return resolver.parseStarlark(value, Glob.class);
              } catch (CannotConvertException e) {
                throw new CannotConvertException(
                    String.format(
                        "Invalid value '%s'for a glob. Use a value like '%s'. Error: %s",
                        value, Glob.ALL_FILES, e.getMessage()));
              }
            }
          });

  public static final Input<Author> DEFAULT_AUTHOR = Input.create(
      "default_author", "Default author for changes",
      null, Author.class, (value, resolver) -> {
        try {
          return AuthorParser.parse(value);
        } catch (InvalidAuthorException e) {
          throw new CannotConvertException(
              "Invalid author. Format \"foo <foo@example.com>\": " + e.getMessage());
        }
      });

  public static final Input<Path> GENERATOR_FOLDER = Input.create(
      "generator_folder", "The folder where the assets will be created",
      null, Path.class, (first, resolver) -> Paths.get(first));

  public static final Input<String> MIGRATION_NAME = Input.create(
      "migration_name", "Migration name",
      null, String.class, (s, resolver) -> s);

  @SuppressWarnings("unused")
  public static final Input<String> PACKAGE_NAME =
      Input.create(
          "package_name",
          "The name of the package to import",
          null,
          String.class,
          (value, resolver) -> value);

  @SuppressWarnings("unused")
  public static final Input<String> PACKAGE_DESCRIPTION =
      Input.create(
          "package_description",
          "The description of the package to import",
          null,
          String.class,
          (value, resolver) -> value);

  private static Input<ConfigGenerator> template;
  public static Input<ConfigGenerator> templateInput() {
    return checkNotNull(template, "Template input has to be set before call");
  }

  public static void maybeSetTemplates(List<ConfigGenerator> values) {
    if (template != null) {
      return;
    }
    ImmutableMap<String, ConfigGenerator> templates = Maps.uniqueIndex(values, v -> v.name());
    template = Input.create(
        "template_name", "Template to use for generating the config",
        null, ConfigGenerator.class, (s, resolver) -> {
          ConfigGenerator configGenerator = templates.get(s);
          if (configGenerator!=null) {
            return configGenerator;
          }
          throw new CannotConvertException(
              String.format("Invalid template '%s'. Available templates: %s",
                  s, Joiner.on(", ").join(templates.keySet())));
        });
  }
}
