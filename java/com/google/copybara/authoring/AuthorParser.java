/*
 * Copyright (C) 2016 Google Inc.
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

package com.google.cooperbara.authoring;

import static com.google.common.base.Throwables.throwIfInstanceOf;
import static com.google.common.base.Throwables.throwIfUnchecked;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.util.concurrent.ExecutionException;

/**
 * A parser for the standard author format {@code "Name <email>"}.
 *
 * <p>This is the format used by most VCS (Git, Mercurial) and also by the Copybara configuration
 * itself. The parser is lenient: {@code email} can be empty, and it doesn't validate that is an
 * actual email.
 */
public class AuthorParser {

  private static final Pattern AUTHOR_PATTERN =
      Pattern.compile("(?P<name>[^<]+)<(?P<email>[^>]*)>");
  private static final Pattern IN_QUOTES =
      Pattern.compile("(\".+\")|(\'.+\')");

  private static final LoadingCache<String, Author> CACHE =
      CacheBuilder.newBuilder()
          .maximumSize(1000000)
          .build(
              new CacheLoader<String, Author>() {
                @Override
                public Author load(String key) throws Exception {
                  return internalParse(key);
                }
              });
  /**
   * Parses a Git author {@code string} into an {@link Author}.
   */
  public static Author parse(String author) throws InvalidAuthorException {
    Preconditions.checkNotNull(author);
    try {
      // Use a cache since repetitive load (thru --read-config-from-change) configs that
      // define authors have a penalty because of the regex check/group.
      return CACHE.get(author);
    } catch (ExecutionException e) {
      throwIfInstanceOf(e.getCause(), InvalidAuthorException.class);
      throwIfUnchecked(e.getCause());
      throw new IllegalStateException(e);
    }
  }

  private static Author internalParse(String author) throws InvalidAuthorException {
    if (IN_QUOTES.matcher(author).matches()) {
      author = author.substring(1, author.length() - 1); //strip quotes
    }
    Matcher matcher = AUTHOR_PATTERN.matcher(author);
    if (matcher.matches()) {
      return new Author(matcher.group(1).trim(), matcher.group(2).trim());
    }
    throw new InvalidAuthorException(
        String.format("Invalid author '%s'. Must be in the form of 'Name <email>'", author));
  }
}
