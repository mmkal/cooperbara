/*
 * Copyright (C) 2016 Google Inc.
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

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.cooperbara.effect.DestinationEffect;
import com.google.cooperbara.exception.RepoException;
import com.google.cooperbara.exception.ValidationException;
import com.google.cooperbara.revision.Revision;
import com.google.cooperbara.util.Glob;
import com.google.cooperbara.util.console.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.StarlarkValue;

/** A repository which a source of truth can be copied to. */
@StarlarkBuiltin(
    name = "destination",
    doc = "A repository which a source of truth can be copied to",
    documented = true)
public interface Destination<R extends Revision> extends ConfigItemDescription, StarlarkValue {

  /**
   * An object which is capable of writing multiple revisions to the destination. This object is
   * allowed to maintain state between the writing of revisions if applicable (for instance, to
   * create multiple changes which are dependent on one another that require review before
   * submission).
   *
   * <p>A single instance of this class is used to import either a single change, or a sequence of
   * changes where each change is the following change's parent.
   */
  interface Writer<R extends Revision> extends ChangeVisitable<R> {

    /**
     * Returns the status of the import at the destination.
     *
     * <p>This method may have undefined behavior if called after {@link #write(TransformResult,
     * Glob, Console)}.
     *
     * @param labelName the label used in the destination for storing the last migrated ref
     * @param destinationFiles the glob to use for filtering changes (optional)
     */
    @Nullable
    DestinationStatus getDestinationStatus(Glob destinationFiles, String labelName)
        throws RepoException, ValidationException;

    /**
     * Returns true if this destination stores revisions in the repository so that
     * {@link #getDestinationStatus(Glob, String)}  can be used for discovering the state of the
     * destination and we can use the methods in {@link ChangeVisitable}.
     */
    boolean supportsHistory();

    /**
     * Writes the fully-transformed repository stored at {@code workdir} to this destination.
     * @param transformResult what to write to the destination
     * @param destinationFiles the glob to use for write. This glob might be different from the
     * one received in {@code {@link #getDestinationStatus(Glob, String)}} due to read config from
     * change configuration.
     * @param console console to be used for printing messages
     * @return one or more destination effects
     *
     * @throws ValidationException if an user attributable error happens during the write
     * @throws RepoException if there was an issue with the destination repository
     * @throws IOException if a file access error happens during the write
     */
    ImmutableList<DestinationEffect> write(TransformResult transformResult, Glob destinationFiles,
        Console console) throws ValidationException, RepoException, IOException;

    /**
     * Utility endpoint for accessing and adding feedback data.
     * @param console console to use for reporting information to the user
     */
    default Endpoint getFeedbackEndPoint(Console console) throws ValidationException {
      return Endpoint.NOOP_ENDPOINT;
    }

    default DestinationReader getDestinationReader(
        Console console, @Nullable Origin.Baseline<?> baseline, Path workdir)
        throws ValidationException, RepoException {
      return DestinationReader.NOT_IMPLEMENTED;
    }

    default DestinationReader getDestinationReader(
        Console console, @Nullable String baseline, Path workdir)
        throws ValidationException, RepoException {
      return DestinationReader.NOT_IMPLEMENTED;
    }

    default Optional<PatchRegenerator> getPatchRegenerator(Console console) {
      return Optional.empty();
    }

    /**
     * Returns the {@link DestinationInfo} object for this destination.
     *
     * <p>This object will be used to store configuration values and other information specific to
     * the destination.
     *
     * @return a DestinationInfo object if the destination supports it, null otherwise.
     */
    @Nullable
    default DestinationInfo getDestinationInfo() {
      return null;
    }
  }

  /** Writers that implement PatchRegenerator can be used with RegenerateCmd */
  interface PatchRegenerator {
    /**
     * Write the files in the workdir to an already-existing change created by Copybara. This is
     * used to update a pending change with new patch files. Implementations should not update
     * anything in the destination other than files.
     */
    default void updateChange(
        String workflowName, Path workdir, Glob destinationFiles, String changeToUpdate)
        throws ValidationException, RepoException {
      throw new ValidationException("update change not implemented for this destination");
    }

    /**
     * Detect regen baseline when not supplied by CLI. The regen baseline is a string referencing a
     * previous patch-consistent state, e.g. a change created by running a workflow. Should be
     * called for migrations that are using the consistency file.
     */
    default Optional<String> inferRegenBaseline() throws ValidationException {
      return Optional.empty();
    }

    /**
     * Detect regen target when not supplied by CLI The regen target is a string referencing a
     * non-cooperbara edited patch-inconsistent state for which patch files need to be regenerated,
     * i.e. a pending change with edits to patch-modified files.
     */
    default Optional<String> inferRegenTarget() throws ValidationException, RepoException {
      return Optional.empty();
    }

    /**
     * Detect import baseline when not supplied by CLI. The import baseline is the origin version
     * that is being imported. Should be called for migrations that are using an import baseline for
     * merge import.
     */
    default Optional<String> inferImportBaseline(String regenTarget, Path workdir)
        throws RepoException, ValidationException {
      return Optional.empty();
    }
  }

  /**
   * Creates a writer which is capable of writing to this destination. This writer may maintain
   * state between writing of revisions.
   *
   * <p>This method should only do trivial initialization of the writer, since it does not have
   * access to a {@link Console}.
   *
   * @param writerContext Contains all the information for writing to destination, including
   *     workflowName, destinationFiles, * dryRun, revision, and oldWriter
   * @throws ValidationException if the writer could not be created because of a user error. For
   *     instance, the destination cannot be used with the given {@code destinationFiles}.
   */
  Writer<R> newWriter(WriterContext writerContext) throws ValidationException;

  /**
   * Given a reverse workflow with an {@code Origin} than is of the same type as this destination,
   * the label that that {@link Origin#getLabelName()} would return.
   *
   * <p>This label name is used by the origin in the reverse workflow to stamp it's original
   * revision id. Destinations return the origin label so that a baseline label can be found when
   * using {@link WorkflowMode#CHANGE_REQUEST}.
   */
  String getLabelNameWhenOrigin() throws ValidationException;

  /**
   * A hash function that is preferred by the Destination for uses cases where hashing is involved,
   * e.g. {@link com.google.cooperbara.util.ConsistencyFile}
   */
  default HashFunction getHashFunction() {
    return Hashing.sha256();
  }

  /**
   * This class represents the status of the destination. It includes the baseline revision and if
   * it is a code review destination, the list of pending changes that have been already migrated.
   * In order: First change is the oldest one.
   */
  final class DestinationStatus {

    private final String baseline;
    private final ImmutableList<String> pendingChanges;

    public DestinationStatus(String baseline, ImmutableList<String> pendingChanges) {
      this.baseline = Preconditions.checkNotNull(baseline);
      this.pendingChanges = Preconditions.checkNotNull(pendingChanges);
    }

    /**
     * String representation of the latest migrated revision in the baseline.
     */
    public String getBaseline() {
      return Preconditions.checkNotNull(baseline, "Trying to get baseline for NO_STATUS");
    }

    /**
     * String representation of the migrated revisions that are in pending state in the destination.
     * First element is the oldest one. Last element the newest one.
     */
    public ImmutableList<String> getPendingChanges() {
      return Preconditions.checkNotNull(pendingChanges,
                                        "Trying to get pendingChanges for NO_STATUS");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DestinationStatus that = (DestinationStatus) o;
      return Objects.equals(baseline, that.baseline)
          && Objects.equals(pendingChanges, that.pendingChanges);
    }

    @Override
    public int hashCode() {
      return Objects.hash(baseline, pendingChanges);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("baseline", baseline)
          .add("pendingChanges", pendingChanges)
          .toString();
    }
  }

}
