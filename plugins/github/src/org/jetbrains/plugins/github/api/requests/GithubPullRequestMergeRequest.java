// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.github.api.requests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.github.api.data.GithubPullRequestMergeMethod;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GithubPullRequestMergeRequest {
  @NotNull private final String commitTitle;
  @NotNull private final String commitMessage;
  @NotNull private final String sha;
  @NotNull private final GithubPullRequestMergeMethod method;

  public GithubPullRequestMergeRequest(@NotNull String commitTitle,
                                       @NotNull String commitMessage,
                                       @NotNull String sha,
                                       @NotNull GithubPullRequestMergeMethod method) {
    if (method != GithubPullRequestMergeMethod.merge && method != GithubPullRequestMergeMethod.squash) {
      throw new IllegalArgumentException("Invalid merge method");
    }

    this.commitTitle = commitTitle;
    this.commitMessage = commitMessage;
    this.sha = sha;
    this.method = method;
  }
}
