/*
 * Copyright (C) 2023 Google LLC
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
package com.google.cooperbara.regenerate;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.annotations.VisibleForTesting;
import com.google.cooperbara.Option;
import java.util.Optional;
import javax.annotation.Nullable;

/** RegenerateOptions modifies behavior of RegenerateCmd */
@Parameters(separators = "=")
public class RegenerateOptions implements Option {

  @VisibleForTesting
  public RegenerateOptions() {}

  public Optional<String> getRegenBaseline() {
    return Optional.ofNullable(regenBaseline);
  }

  public boolean getUseImportBaseline() {
    return regenImportBaseline;
  }

  public Optional<String> getRegenTarget() {
    return Optional.ofNullable(regenTarget);
  }

  @Nullable
  @Parameter(
      names = "--regen-baseline",
      description = "a value identifying a destination revision with consistent patch files state")
  private String regenBaseline;

  @VisibleForTesting
  public void setRegenBaseline(@Nullable String regenBaseline) {
    this.regenBaseline = regenBaseline;
  }

  @Parameter(
      names = "--regen-import-baseline",
      arity = 1,
      description = "create the baseline by doing a workflow import")
  private boolean regenImportBaseline = false;

  @VisibleForTesting
  public void setRegenImportBaseline(boolean regenImportBaseline) {
    this.regenImportBaseline = regenImportBaseline;
  }

  @Nullable
  @Parameter(
      names = "--regen-target",
      description =
          "a value identifying the current destination revision to generate patch files against")
  private String regenTarget;

  @VisibleForTesting
  public void setRegenTarget(@Nullable String regenTarget) {
    this.regenTarget = regenTarget;
  }
}
