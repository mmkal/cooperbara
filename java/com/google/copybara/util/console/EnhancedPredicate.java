/*
 * Copyright (C) 2021 Google Inc.
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

package com.google.cooperbara.util.console;

import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;

/** Enhanced Predicate object for use with Copybara console objects */
@AutoValue
public abstract class EnhancedPredicate {

  public static EnhancedPredicate create(Predicate<String> predicate, String errorMsg) {
    return new AutoValue_EnhancedPredicate(predicate, errorMsg);
  }

  public abstract Predicate<String> predicate();

  public abstract String errorMsg();
}
