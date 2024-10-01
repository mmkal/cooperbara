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

package com.google.cooperbara.templatetoken;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * Either a string literal or interpolated value.
 */
public final class Token {
  private final String value;
  private final TokenType type;

  Token(String value, TokenType type) {
    this.value = Preconditions.checkNotNull(value);
    this.type = Preconditions.checkNotNull(type);
  }

  /** Create an interpolation token */
  public static Token interpolation(String name) {
    return new Token(name, TokenType.INTERPOLATION);
  }

  /** Create a literal token */
  public static Token literal(String name) {
    return new Token(name, TokenType.LITERAL);
  }

  public String getValue() {
    return value;
  }

  public TokenType getType() {
    return type;
  }

  /**
   * The type of the token
   */
  public enum TokenType {
    LITERAL, INTERPOLATION
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("value", value)
        .add("type", type)
        .toString();
  }
}
