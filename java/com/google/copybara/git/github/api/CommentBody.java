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

package com.google.cooperbara.git.github.api;

import com.google.api.client.util.Key;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Request type for https://docs.github.com/en/rest/reference/issues#create-an-issue-comment
 */
public class CommentBody {
  @Key @Nullable String body;

  public CommentBody(String body) {
    this.body = body;
  }

  public CommentBody() {
  }

  public Optional<String> getBody() {
    return Optional.ofNullable(body);
  }
}
