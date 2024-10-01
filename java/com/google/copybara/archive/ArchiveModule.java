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

package com.google.cooperbara.archive;

import static com.google.common.io.Files.getFileExtension;

import com.google.cooperbara.CheckoutPath;
import com.google.cooperbara.config.SkylarkUtil;
import com.google.cooperbara.exception.ValidationException;
import com.google.cooperbara.remotefile.extractutil.ExtractType;
import com.google.cooperbara.remotefile.extractutil.ExtractUtil;
import com.google.cooperbara.util.Glob;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkValue;

/**
 * A module for handling archives in Starlark.
 */
@StarlarkBuiltin(
    name = "archive",
    doc = "Functions to work with archives."
)
public class ArchiveModule implements StarlarkValue {

  public ArchiveModule() {}

  @StarlarkMethod(
      name = "extract",
      doc = "Extract the contents of the archive to a path.",
      documented = true,
      parameters = {
          @Param(
              name = "archive",
              named = true,
              doc = "The path to the archive file."
          ),
          @Param(
              name = "type",
              named = true,
              doc = "The archive type. Supported types: AUTO, JAR, ZIP, TAR, and TAR_GZ. AUTO will"
                  + " try to infer the archive type automatically.",
              defaultValue = "\"AUTO\""
          ),
          @Param(
              name = "destination_folder",
              named = true,
              doc = "The path to extract the archive to. This defaults to the directory where the"
                  + " archive is located.",
              defaultValue = "None",
              allowedTypes = {
                  @ParamType(type = CheckoutPath.class),
                  @ParamType(type = NoneType.class),
              }
          ),
          @Param(
              name = "paths",
              named = true,
              doc = "An optional glob that is used to filter the files extracted from the "
                  + "archive.",
              defaultValue = "None",
              allowedTypes = {
                  @ParamType(type = Glob.class),
                  @ParamType(type = NoneType.class),
              }
          )
      }
  )
  @SuppressWarnings("unused")
  public void extract(
      CheckoutPath archivePath, String typeStr, Object maybeDestination, Object paths)
      throws EvalException {
    ExtractType type;

    if (typeStr.equals("AUTO")) {
      type = resolveArchiveType(archivePath);
    } else {
      type = SkylarkUtil.stringToEnum("type", typeStr, ExtractType.class);
    }
    CheckoutPath destination = SkylarkUtil.convertFromNoneable(maybeDestination,
        archivePath.resolve(".."));

    try (InputStream contents = Files.newInputStream(archivePath.fullPath())) {
      ExtractUtil.extractArchive(
          contents,
          destination.fullPath(),
          type,
          SkylarkUtil.convertFromNoneable(paths, null));
    } catch (IOException | ValidationException e) {
      throw Starlark.errorf("There was an error extracting the archive: %s", e.toString());
    }
  }
  
  private static ExtractType resolveArchiveType(CheckoutPath archivePath) throws EvalException {
    String extension = getFileExtension(archivePath.getPath().getFileName().toString());
    switch (extension) {
      case "zip":
        return ExtractType.ZIP;
      case "jar":
        return ExtractType.JAR;
      case "tar":
        return ExtractType.TAR;
      case "tgz":
        // fall through
      case "tar.gz":
        return ExtractType.TAR_GZ;
    }

    throw Starlark.errorf("The archive type couldn't be inferred for the file: %s",
        archivePath.getPath().toString());
  }
}
