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

package com.google.cooperbara.doc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to associate flags to functions in SkylarkModules.
 *
 * <p>Can be set to a whole module or specific field functions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,
    ElementType.FIELD, // TODO(malcon): Remove this once everything migrated to @StarlarkMethod
    ElementType.METHOD})
public @interface UsesFlags {

  /**
   * An associated flags class annotated with {@code Parameter}
   */
  // TODO(cooperbara-team): change to <? extends Option>.
  Class<?>[] value();
}
