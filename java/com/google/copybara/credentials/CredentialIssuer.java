/*
 * Copyright (C) 2023 Google LLC.
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
package com.google.cooperbara.credentials;

import com.google.common.collect.ImmutableSetMultimap;
import net.starlark.java.eval.StarlarkValue;

/**
 * An object able to mint credentials. The issuer should handle caching etc.
 */
public interface CredentialIssuer extends StarlarkValue {

  /**
   * Issue a Credential to be used by an endpoint
   */
  Credential issue() throws CredentialIssuingException;

  /**
   * Metadata describing this issuer.
   */
  ImmutableSetMultimap<String, String> describe();
}
