/*
 * Copyright (C) 2020 Google Inc.
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
import javax.annotation.Nullable;

/** see https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-vote-input
 * request json
 * <p>NotifyInfo (notify_details) not included for now
 * <p>label not included for now since it matches the label in the URL.
 */
public class DeleteVoteInput {

  @Key String notify;

  public DeleteVoteInput(@Nullable NotifyType notify) {
    this.notify = notify == null ? null : notify.toString();
  }
}
