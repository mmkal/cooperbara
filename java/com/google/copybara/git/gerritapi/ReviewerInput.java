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

package com.google.cooperbara.git.gerritapi;

import com.google.api.client.util.Key;
import com.google.api.client.util.Preconditions;

/** see https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#reviewer-input
 * request json
 */
public class ReviewerInput {

  @Key private String reviewer;

  public ReviewerInput(String reviewer) {
    this.reviewer = Preconditions.checkNotNull(reviewer);
  }

}
