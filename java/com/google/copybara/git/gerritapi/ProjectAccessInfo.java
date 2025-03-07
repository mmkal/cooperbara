/*
 * Copyright (C) 2017 Google Inc.
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

package com.google.cooperbara.git.gerritapi;

import com.google.api.client.util.Key;
import com.google.common.base.MoreObjects;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkValue;

/**
 * Relevant field(s) of the ProjectAccessInfo message.
 * https://gerrit-review.googlesource.com/Documentation/rest-api-access.html#project-access-info
 */
public class ProjectAccessInfo implements StarlarkValue {
  @Key("is_owner") boolean isOwner;

  public boolean isOwner() {
    return isOwner;
  }

  @Override
  public void repr(Printer printer) {
    printer.append(toString());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("is_owner", isOwner)
        .toString();
  }
}
