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

import static com.google.cooperbara.exception.ValidationException.checkCondition;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.cooperbara.AutoPatchfileConfiguration;
import com.google.cooperbara.Destination.PatchRegenerator;
import com.google.cooperbara.Destination.Writer;
import com.google.cooperbara.DestinationReader;
import com.google.cooperbara.GeneralOptions;
import com.google.cooperbara.Origin.Baseline;
import com.google.cooperbara.Workflow;
import com.google.cooperbara.WorkflowMode;
import com.google.cooperbara.WorkflowOptions;
import com.google.cooperbara.WorkflowRunHelper;
import com.google.cooperbara.WriterContext;
import com.google.cooperbara.exception.RepoException;
import com.google.cooperbara.exception.ValidationException;
import com.google.cooperbara.monitor.EventMonitor.ChangeMigrationFinishedEvent;
import com.google.cooperbara.revision.Revision;
import com.google.cooperbara.util.AutoPatchUtil;
import com.google.cooperbara.util.ConsistencyFile;
import com.google.cooperbara.util.Glob;
import com.google.cooperbara.util.InsideGitDirException;
import com.google.cooperbara.util.console.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Regenerate contains the implementation of the logic to checkout the correct versions of code and
 * calling the helper classes to diff and upload the contents.
 */
public class Regenerate<O extends Revision, D extends Revision> {

  Console console;
  @Nullable AutoPatchfileConfiguration autoPatchfileConfiguration;
  Workflow<O, D> workflow;
  Path workdir;
  GeneralOptions generalOptions;
  WorkflowOptions workflowOptions;
  RegenerateOptions regenerateOptions;
  @Nullable String sourceRef;

  public static Regenerate<? extends Revision, ? extends Revision> newRegenerate(
      Workflow<? extends Revision, ? extends Revision> workflow,
      Path workdir,
      GeneralOptions generalOptions,
      WorkflowOptions workflowOptions,
      RegenerateOptions regenerateOptions,
      @Nullable String sourceRef) {
    return new Regenerate<>(
        workflow, workdir, generalOptions, workflowOptions, regenerateOptions, sourceRef);
  }

  public Regenerate(
      Workflow<O, D> workflow,
      Path workdir,
      GeneralOptions generalOptions,
      WorkflowOptions workflowOptions,
      RegenerateOptions regenerateOptions,
      @Nullable String sourceRef) {
    this.workflow = workflow;
    this.workdir = workdir;
    this.generalOptions = generalOptions;
    this.workflowOptions = workflowOptions;
    this.regenerateOptions = regenerateOptions;
    this.console = generalOptions.console();
    this.sourceRef = sourceRef;
    this.autoPatchfileConfiguration = workflow.getAutoPatchfileConfiguration();
  }

  @VisibleForTesting
  public void regenerate() throws ValidationException, RepoException, IOException {

    Writer<D> destinationWriter =
        workflow
            .getDestination()
            .newWriter(
                new WriterContext(
                    workflow.getName(),
                    workflowOptions.workflowIdentityUser,
                    generalOptions.dryRunMode,
                    workflow.getOrigin().resolve(null),
                    workflow.getDestinationFiles().roots()));
    PatchRegenerator patchRegenerator =
        destinationWriter
            .getPatchRegenerator(generalOptions.console())
            .orElseThrow(
                () ->
                    new ValidationException(
                        "this destination does not support regenerating patch files"));

    // use the same directory names as workflow
    // TODO(b/296111124)
    Path previousPath = workdir.resolve("premerge");
    Path nextPath = workdir.resolve("checkout");

    Path autopatchPath = workdir.resolve("autopatches");
    Files.createDirectories(previousPath);
    Files.createDirectories(nextPath);
    Files.createDirectories(autopatchPath);

    Optional<String> getRegenTargetResult = regenerateOptions.getRegenTarget();
    if (getRegenTargetResult.isEmpty()) {
      getRegenTargetResult = patchRegenerator.inferRegenTarget();
    }
    String regenTarget =
        getRegenTargetResult.orElseThrow(
            () ->
                new ValidationException(
                    "Regen target was neither supplied nor able to be inferred. Supply with"
                        + " --regen-target parameter"));
    AutoPatchfileConfiguration autopatchConfig = workflow.getAutoPatchfileConfiguration();

    Optional<String> regenBaseline = Optional.empty();
    if (workflow.isConsistencyFileMergeImport()) {
      regenBaseline = regenerateOptions.getRegenBaseline();
      if (regenBaseline.isEmpty()) {
        regenBaseline = patchRegenerator.inferRegenBaseline();
      }
      if (regenBaseline.isEmpty()) {
        console.info("Regen baseline could not be inferred. Falling back to import baseline");
      }
    }

    if (regenBaseline.isPresent()) {
      checkCondition(
          consistencyFileExists(
              destinationWriter, regenBaseline.get(), workflow.getConsistencyFilePath()),
          "Regenerating a consistency file merge import change but no consistency file found.");
      prepareDiffWithConsistencyFileBaseline(
          autopatchConfig,
          workflow,
          destinationWriter,
          previousPath,
          nextPath,
          autopatchPath,
          regenBaseline.get(),
          regenTarget);
    } else {
      previousPath =
          prepareDiffWithImportBaseline(
              patchRegenerator,
              autopatchConfig,
              workflow,
              workdir,
              nextPath,
              regenTarget,
              destinationWriter);
    }

    Optional<byte[]> consistencyFile = Optional.empty();
    if (workflow.isConsistencyFileMergeImport()) {
      try {
        consistencyFile =
            Optional.of(
                ConsistencyFile.generate(
                        previousPath,
                        nextPath,
                        workflow.getDestination().getHashFunction(),
                        workflow.getGeneralOptions().getEnvironment(),
                        workflow.isVerbose())
                    .toBytes());
      } catch (InsideGitDirException e) {
        throw new ValidationException("Error generating consistency file", e);
      }
    }

    if (autopatchConfig != null) {
      // generate new autopatch files in the target directory
      try {
        AutoPatchUtil.generatePatchFiles(
            previousPath,
            nextPath,
            Path.of(autopatchConfig.directoryPrefix()),
            autopatchConfig.directory(),
            workflow.isVerbose(),
            workflow.getGeneralOptions().getEnvironment(),
            autopatchConfig.header(),
            autopatchConfig.suffix(),
            nextPath,
            autopatchConfig.stripFilenames(),
            autopatchConfig.stripLineNumbers(),
            autopatchConfig.glob());
      } catch (InsideGitDirException e) {
        throw new ValidationException(
            String.format(
                "Could not automatically generate patch files because temporary directory %s is"
                    + " inside git repository %s. Error received is %s",
                e.getPath(), e.getGitDirPath(), e.getMessage()),
            e);
      }
    }

    if (workflow.getConsistencyFilePath() != null && consistencyFile.isPresent()) {
      Files.createDirectories(nextPath.resolve(workflow.getConsistencyFilePath()).getParent());
      Files.write(nextPath.resolve(workflow.getConsistencyFilePath()), consistencyFile.get());
    }

    // push the new set of files
    patchRegenerator.updateChange(
        workflow.getName(), nextPath, workflow.getDestinationFiles(), regenTarget);
  }

  private boolean consistencyFileExists(
      Writer<D> destinationWriter, String regenBaseline, String consistencyFilePath)
      throws ValidationException, RepoException {
    DestinationReader previousDestinationReader =
        destinationWriter.getDestinationReader(console, regenBaseline, workdir);
    return previousDestinationReader.exists(consistencyFilePath);
  }

  private void prepareDiffWithConsistencyFileBaseline(
      @Nullable AutoPatchfileConfiguration autopatchConfig,
      Workflow<O, D> workflow,
      Writer<D> destinationWriter,
      Path previousPath,
      Path nextPath,
      Path patchPath,
      String regenBaseline,
      String regenTarget)
      throws ValidationException, RepoException, IOException {

    Glob patchlessDestinationFiles = workflow.getDestinationFiles();

    // download all files except for patch files
    if (autopatchConfig != null) {
      Glob autopatchGlob =
          AutoPatchUtil.getAutopatchGlob(
              autopatchConfig.directoryPrefix(), autopatchConfig.directory());
      patchlessDestinationFiles = Glob.difference(patchlessDestinationFiles, autopatchGlob);
    }

    Glob consistencyFileGlob = Glob.createGlob(ImmutableList.of(workflow.getConsistencyFilePath()));
    patchlessDestinationFiles = Glob.difference(patchlessDestinationFiles, consistencyFileGlob);

    // cooper the baseline to one directory
    DestinationReader previousDestinationReader =
        destinationWriter.getDestinationReader(console, regenBaseline, workdir);
    previousDestinationReader.cooperDestinationFilesToDirectory(
        patchlessDestinationFiles, previousPath);

    // cooper the target to another directory
    DestinationReader nextDestinationReader =
        destinationWriter.getDestinationReader(console, regenTarget, workdir);
    nextDestinationReader.cooperDestinationFilesToDirectory(patchlessDestinationFiles, nextPath);

    // cooper consistency file to a third directory
    previousDestinationReader.cooperDestinationFilesToDirectory(consistencyFileGlob, patchPath);

    // reverse patch files on the target directory here to get a pristine import
    Path consistencyFilePath = patchPath.resolve(workflow.getConsistencyFilePath());
    if (Files.exists(consistencyFilePath)) {
      ConsistencyFile consistencyFile =
          ConsistencyFile.fromBytes(Files.readAllBytes(consistencyFilePath));
      consistencyFile.reversePatches(previousPath, workflow.getGeneralOptions().getEnvironment());
    } else {
      console.warn("ConsistencyFile enabled but no ConsistencyFile file encountered");
    }
  }

  private Path prepareDiffWithImportBaseline(
      PatchRegenerator patchRegenerator,
      @Nullable AutoPatchfileConfiguration autopatchConfig,
      Workflow<O, D> workflow,
      Path workdir,
      Path nextPath,
      String regenTarget,
      Writer<D> destinationWriter)
      throws ValidationException, RepoException, IOException {

    WorkflowRunHelper<O, D> runHelper;
    O importRevision;

    if (sourceRef == null) {
      // no source ref specified, attempt to infer
      Optional<String> inferImportBaselineResult =
          patchRegenerator.inferImportBaseline(regenTarget, workdir);
      if (inferImportBaselineResult.isPresent()) {
        console.infoFmt(
            "Inferred import baseline %s from destination", inferImportBaselineResult.get());
        importRevision = workflow.getOrigin().resolve(inferImportBaselineResult.get());
        runHelper =
            createRunHelper(workflow, workdir, importRevision, inferImportBaselineResult.get());
      } else {
        // no source ref, no inferred baseline
        console.warn(
            "Regenerate was unable to detect the import baseline reference nor was a reference"
                + " passed in.\n"
                + "Ideally, the reference imported by the workflow migration is the one used for"
                + " the import baseline.\n"
                + "Regenerate will use the latest reference or follow `--same-version`, but this"
                + " may not match the one used for the initial import\n"
                + "To pass in a reference, add it to the cooperbara command, e.g. `cooperbara"
                + " regenerate [config path] [migration name] [reference]`\n");
        // use workflow logic to determine reference
        importRevision = workflow.getOrigin().resolve(sourceRef);
        runHelper = createRunHelper(workflow, workdir, importRevision, sourceRef);

        if (WorkflowMode.isHistorySupported(runHelper)) {
          if (workflowOptions.importSameVersion) {
            importRevision = WorkflowMode.maybeGetLastRev(runHelper);
          }
        }

        console.infoFmt(
            "Regenerating with import baseline from origin revision %s", importRevision.asString());
      }
    } else {
      console.infoFmt("Regenerating with import baseline from source ref %s", sourceRef);
      importRevision = workflow.getOrigin().resolve(sourceRef);
      runHelper = createRunHelper(workflow, workdir, importRevision, sourceRef);
    }

    Glob patchlessDestinationFiles = workflow.getDestinationFiles();
    if (autopatchConfig != null) {
      Glob autopatchGlob =
          AutoPatchUtil.getAutopatchGlob(
              autopatchConfig.directoryPrefix(), autopatchConfig.directory());
      patchlessDestinationFiles = Glob.difference(workflow.getDestinationFiles(), autopatchGlob);
    }
    if (workflow.getConsistencyFilePath() != null) {
      Glob consistencyFileGlob =
          Glob.createGlob(ImmutableList.of(workflow.getConsistencyFilePath()));
      patchlessDestinationFiles = Glob.difference(patchlessDestinationFiles, consistencyFileGlob);
    }

    // cooper the baseline to one directory
    DestinationReader previousDestinationReader =
        destinationWriter.getDestinationReader(console, (Baseline<?>) null, workdir);
    Path importPath =
        runHelper.importAndTransformRevision(
            console, null, importRevision, () -> previousDestinationReader);

    // cooper the target to another directory
    DestinationReader nextDestinationReader =
        destinationWriter.getDestinationReader(console, regenTarget, workdir);
    nextDestinationReader.cooperDestinationFilesToDirectory(patchlessDestinationFiles, nextPath);

    return importPath;
  }

  private WorkflowRunHelper<O, D> createRunHelper(
      Workflow<O, D> workflow, Path workdir, O resolvedRef, String sourceRef)
      throws ValidationException {
    return workflow.newRunHelper(
        workdir, resolvedRef, sourceRef, (ChangeMigrationFinishedEvent e) -> {});
  }
}
