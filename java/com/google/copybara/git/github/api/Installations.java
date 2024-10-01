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

package com.google.cooperbara.git.github.api;

import com.google.api.client.util.Key;
import com.google.common.base.MoreObjects;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Correspond to JSON schema response for top level object in
 * https://docs.github.com/en/rest/orgs/orgs#list-app-installations-for-an-organization
 *
 * <p>Not all property keys are included here. Add them as needed.
 */
public class Installations implements PaginatedPayload<Installation> {
  @Key("total_count")
  private int installionsCount;

  @Key("installations")
  private PaginatedList<Installation> installations;

  public Installations() {}

  public Installations(int installionsCount, PaginatedList<Installation> installations) {
    this.installionsCount = installionsCount;
    this.installations = installations;
  }

  public int getInstallionsCount() {
    return installionsCount;
  }

  public List<Installation> getInstallations() {
    return installations;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("installations_count", installionsCount)
        .add("installations", installations)
        .toString();
  }

  @Override
  public PaginatedList<Installation> getPayload() {
    return installations;
  }

  @Override
  public PaginatedPayload<Installation> annotatePayload(
      String apiPrefix, @Nullable String linkHeader) {
    return new Installations(
        installionsCount, installations.withPaginationInfo(apiPrefix, linkHeader));
  }
}
