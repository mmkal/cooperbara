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
package com.google.cooperbara.git;

import static com.google.cooperbara.exception.ValidationException.checkCondition;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.flogger.FluentLogger;
import com.google.cooperbara.Endpoint;
import com.google.cooperbara.GeneralOptions;
import com.google.cooperbara.checks.Checker;
import com.google.cooperbara.effect.DestinationEffect;
import com.google.cooperbara.effect.DestinationEffect.DestinationRef;
import com.google.cooperbara.effect.DestinationEffect.Type;
import com.google.cooperbara.exception.RepoException;
import com.google.cooperbara.exception.ValidationException;
import com.google.cooperbara.git.GitDestination.MessageInfo;
import com.google.cooperbara.git.GitDestination.WriterImpl.DefaultWriteHook;
import com.google.cooperbara.git.github.api.GitHubApi;
import com.google.cooperbara.git.github.api.GitHubApiException;
import com.google.cooperbara.git.github.api.GitHubApiException.ResponseCode;
import com.google.cooperbara.git.github.util.GitHubHost;
import com.google.cooperbara.git.github.util.GitHubUtil;
import com.google.cooperbara.revision.Change;
import com.google.cooperbara.templatetoken.LabelTemplate;
import com.google.cooperbara.templatetoken.LabelTemplate.LabelNotFoundException;
import com.google.cooperbara.util.console.Console;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.starlark.java.eval.Dict;

public class GitHubWriteHook extends DefaultWriteHook {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final String repoUrl;
  private final GeneralOptions generalOptions;
  private final GitHubOptions gitHubOptions;
  private final boolean deletePrBranch;
  private final Console console;
  @Nullable private final Checker endpointChecker;
  private final GitHubHost ghHost;
  @Nullable private final String prBranchToUpdate;
  @Nullable private final CredentialFileHandler creds;

  GitHubWriteHook(
      GeneralOptions generalOptions,
      String repoUrl,
      GitHubOptions gitHubOptions,
      @Nullable String prBranchToUpdate,
      boolean deletePrBranch,
      Console console,
      @Nullable Checker endpointChecker,
      GitHubHost ghHost,
      @Nullable CredentialFileHandler creds) {
    this.generalOptions = Preconditions.checkNotNull(generalOptions);
    this.repoUrl = Preconditions.checkNotNull(repoUrl);
    this.gitHubOptions = Preconditions.checkNotNull(gitHubOptions);
    this.prBranchToUpdate = prBranchToUpdate;
    this.deletePrBranch = deletePrBranch;
    this.console = console;
    this.endpointChecker = endpointChecker;
    this.ghHost = ghHost;
    this.creds = creds;
  }

  @Override
  public void beforePush(
      GitRepository scratchClone,
      MessageInfo messageInfo,
      boolean skipPush,
      List<? extends Change<?>> originChanges)
      throws ValidationException, RepoException {
    if (skipPush || prBranchToUpdate == null) {
      return;
    }
    String configProjectName = ghHost.getProjectNameFromUrl(repoUrl);
    GitHubApi api = gitHubOptions.newGitHubRestApi(configProjectName, creds);

    for (Change<?> change : originChanges) {
      Dict<String, String> labelDict = change.getLabelsForSkylark();
      String updatedPrBranchName = getUpdatedPrBranch(labelDict);
      String completeRef = String.format("refs/heads/%s", updatedPrBranchName);
      try {
        //fails with NOT_FOUND if doesn't exist
        api.getReference(configProjectName, completeRef);
        generalOptions.repoTask(
            "push current commit to the head of pr_branch_to_update",
            () ->
                scratchClone
                    .push()
                    .withRefspecs(
                        repoUrl,
                        ImmutableList.of(
                            scratchClone.createRefSpec("+HEAD:" + completeRef)))
                    .run());
      } catch (GitHubApiException e) {
        if (e.getResponseCode() == ResponseCode.NOT_FOUND
            || e.getResponseCode() == ResponseCode.UNPROCESSABLE_ENTITY) {
          console.verboseFmt("Branch %s does not exist", updatedPrBranchName);
          logger.atInfo().log("Branch %s does not exist", updatedPrBranchName);
          continue;
        }
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<DestinationEffect> afterPush(String serverResponse, MessageInfo messageInfo,
      GitRevision pushedRevision, List<? extends Change<?>> originChanges)
      throws ValidationException, RepoException {
    ImmutableList.Builder<DestinationEffect> baseEffects =
        ImmutableList.<DestinationEffect>builder()
            .addAll(super.afterPush(serverResponse, messageInfo, pushedRevision, originChanges));
    if (prBranchToUpdate == null || !deletePrBranch) {
      return baseEffects.build();
    }
    String projectId = ghHost.getProjectNameFromUrl(repoUrl);
    GitHubApi api = gitHubOptions.newGitHubRestApi(projectId, creds);

    if (!originChanges.isEmpty()) {
      if (gitHubOptions.githubPrBranchDeletionDelay != null) {
        try {
          SECONDS.sleep(gitHubOptions.githubPrBranchDeletionDelay.getSeconds());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    for (Change<?> change : originChanges) {
      Dict<String, String> labelDict = change.getLabelsForSkylark();
      String updatedPrBranchName = getUpdatedPrBranch(labelDict);
      checkCondition(!Objects.equals(updatedPrBranchName, "master"),
          "Cannot delete 'master' branch from GitHub");

      String completeRef = String.format("refs/heads/%s", updatedPrBranchName);
      try {
        api.deleteReference(projectId, completeRef);
        baseEffects.add(new DestinationEffect(Type.UPDATED,
            String.format("Reference '%s' deleted", completeRef),
            ImmutableList.of(change),
            new DestinationRef(completeRef, "ref_deleted",
                "https://github.com/" + projectId + "/tree/" + updatedPrBranchName)));
      } catch (GitHubApiException e) {
        if (e.getResponseCode() == ResponseCode.NOT_FOUND
            || e.getResponseCode() == ResponseCode.UNPROCESSABLE_ENTITY) {
          console.infoFmt("Branch %s does not exist", updatedPrBranchName);
          logger.atInfo().log("Branch %s does not exist", updatedPrBranchName);
          continue;
        }
        throw e;
        }
      }
    }
    return baseEffects.build();
  }

  @Override
  public Endpoint getFeedbackEndPoint(Console console) throws ValidationException {
    gitHubOptions.validateEndpointChecker(endpointChecker);
    return new GitHubEndPoint(
        gitHubOptions.newGitHubApiSupplier(repoUrl, endpointChecker, creds, ghHost),
        repoUrl,
        console,
        ghHost,
        creds);
  }

  @Override
  public ImmutableSetMultimap<String, String> describe() {
    return prBranchToUpdate == null
        ? ImmutableSetMultimap.of()
        : ImmutableSetMultimap.of("pr_branch_to_update", prBranchToUpdate);
  }

  private String getUpdatedPrBranch(Dict<String, String> labelDict) throws ValidationException {
    try {
      return GitHubUtil.getValidBranchName(
          new LabelTemplate(prBranchToUpdate).resolve(labelDict::get));
    } catch (LabelNotFoundException e) {
      throw new ValidationException(
          String.format("Template '%s' has an error: %s", prBranchToUpdate, e.getMessage()), e);
    }
  }

  public boolean isDeletePrBranch() {
    return deletePrBranch;
  }
}