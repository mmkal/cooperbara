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

import com.google.cooperbara.exception.ValidationException;
import java.io.IOException;
import javax.annotation.Nullable;

/** A class that given a main config path (cooper.bara.sky file) returns a ConfigLoader. */
public interface ConfigLoaderProvider {

  /** Create a new loader for {@code configPath} */
  ConfigLoader newLoader(String configPath, @Nullable String sourceRef)
      throws ValidationException, IOException;

}
