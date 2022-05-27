/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bf2.arch.bot;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkiverse.githubapp.event.PullRequest;
import org.bf2.arch.bot.model.record.RecordId;
import org.bf2.arch.bot.model.record.RecordType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchReviewStateMachineFlow {
    private static final Logger LOG = LoggerFactory.getLogger(ArchReviewStateMachineFlow.class);

    private static final String ENABLE = "bot.enable.state-machine";
    @ConfigProperty(name = ENABLE, defaultValue = "false")
    boolean enabled;

    /**
     * <pre>
     * WHEN
     * * a PR is opened, or
     * * it's draftness is removed, or
     * * it is updated
     * THEN
     * If it touches a record
     *   Add the "type: content" label
     *   If there are no reviewers
     *     Add the "state: needs-reviewers" label
     * Else:
     *   Remove the "type: content" label
     * </pre>
     * Reviewers get added in a MAS arch meeting by triaging a queue like
     * https://github.com/tombentley/app-services-architecture/pulls?q=is%3Aopen+is%3Apr+label%3A%22state%3A+needs-reviewers%22
     */
    public void classifyAndMaybeReviewers(
            @PullRequest.Opened
            @PullRequest.ReadyForReview
            @PullRequest.Edited
            GHEventPayload.PullRequest pullRequestPayload,
            @ConfigFile(Util.CONFIG_REPO_PATH) ArchBotConfig config) throws IOException {
        if (!enabled) {
            LOG.debug("Ignoring event: disabled due to {}=false", ENABLE);
            return;
        }
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        if (!pullRequest.isDraft()
                && !Util.isThisBot(config, pullRequestPayload.getSender())) {
            LOG.info("Processing PR#{}", pullRequest.getId());
            Set<String> existingLabels = Util.existingLabels(pullRequest);
            LOG.info("PR#{}: existing labels {}", pullRequest.getId(), existingLabels);
            Set<String> labels = new HashSet<>(existingLabels);
            EnumSet<RecordType> touchedRecords = touchesRecord(pullRequest);
            LOG.info("PR#{} touches {}", pullRequest.getId(), touchedRecords);
            if (!touchedRecords.isEmpty()) {
                labels.remove(Labels.TYPE_INFRA);
                touchedRecords.forEach(x -> {
                    switch (x) {
                        case AP:
                            labels.add(Labels.TYPE_AP);
                            break;
                        case ADR:
                            labels.add(Labels.TYPE_ADR);
                            break;
                        case PADR:
                            labels.add(Labels.TYPE_PADR);
                            break;
                    }
                });
                if (pullRequest.getRequestedReviewers().isEmpty()) {
                    labels.add(Labels.STATE_NEEDS_REVIEWERS);
                } else {
                    labels.remove(Labels.STATE_NEEDS_REVIEWERS);
                    labels.add(Labels.STATE_BEING_REVIEWED);
                }
            } else {
                labels.remove(Labels.TYPE_ADR);
                labels.remove(Labels.TYPE_AP);
                labels.remove(Labels.TYPE_PADR);
                labels.add(Labels.TYPE_INFRA);
            }

            if (!existingLabels.equals(labels)) {
                LOG.info("PR#{}: updating labels {}", pullRequest.getId(), labels);
                Util.setLabels(pullRequest, labels);
            } else {
                LOG.info("PR#{}: unchanged labels", pullRequest.getId());
            }
        } else {
            LOG.info("Ignoring PR#{}", pullRequest.getId());
        }
    }

    /**
     * Returns true if the PR touches any record (ADR, AP, PADR)
     * @param pullRequest The pull request
     * @return true iff a record file is touched by the commits in the PR.
     */
    EnumSet<RecordType> touchesRecord(GHPullRequest pullRequest) {
        EnumSet<RecordType> touchesRecord = EnumSet.noneOf(RecordType.class);
        var prNumber = pullRequest.getNumber();
        for (var fileDetail : pullRequest.listFiles()) {
            String repoPath = fileDetail.getFilename();
            LOG.info("PR #{} modifies file {}", prNumber, repoPath);
            for (var rt : RecordType.values()) {
                RecordId recordId = rt.recordOf(repoPath);
                if (recordId != null) {
                    touchesRecord.add(rt);
                }
            }
        }
        return touchesRecord;
    }


    enum ReviewerDisposition {
        ACCEPT,
        REJECT,
        DEFER
    }

    /**
     * When
     * a PR comment is added:
     *    If all reviewers have commented "/accept" (or all /defer, or all /reject)
     * THEN
     * 1. Check/Update the status matches the review outcome (accept, defer, reject)
     * 2. Remove "type: being-reviewed" and add "type: ready-for-merge"
     */
    public void readyForMerge(@IssueComment.Created
                              GHEventPayload.IssueComment payload,
                              @ConfigFile(Util.CONFIG_REPO_PATH) ArchBotConfig config) throws IOException, URISyntaxException {
        if (!enabled) {
            LOG.debug("Ignoring event: disabled due to {}=false", ENABLE);
            return;
        }
        GHIssue issue = payload.getIssue();
        if (!issue.isPullRequest()) {
            LOG.debug("Ignoring non-PR issue #{}", issue.getNumber());
            return;
        }
        if (Util.isThisBot(config, payload.getComment().getUser())) {
            LOG.debug("PR#{}: Ignoring my own comment", issue.getNumber());
            return;
        }
        GHPullRequest pullRequest = Util.findPullRequest(issue);
        if (pullRequest == null) {
            return;
        }

        Set<String> reviewers = pullRequest.getRequestedReviewers().stream().map(GHPerson::getLogin).collect(Collectors.toSet());

        if (reviewers.isEmpty()) {
            LOG.debug("PR#{}: Ignoring because it has no reviewers", issue.getNumber());
            return;
        }

        Map<String, ReviewerDisposition> outcomes = new HashMap<>();
        for (var comment : issue.getComments()) {
            String reviewer = comment.getUser().getLogin();
            if (reviewers.contains(reviewer)) {
                // TODO might want to check for these as words, not merely contains
                if (comment.getBody().contains("/accept")) {
                    LOG.debug("PR#{}: {} accepts the changes",
                            issue.getNumber(),reviewer);
                    outcomes.put(reviewer, ReviewerDisposition.ACCEPT);
                } else if(comment.getBody().contains("/defer")) {
                    LOG.debug("PR#{}: {} defers the changes",
                            issue.getNumber(), reviewer);
                    outcomes.put(reviewer, ReviewerDisposition.DEFER);
                } else if (comment.getBody().contains("/reject")) {
                    LOG.debug("PR#{}: {} rejects the changes",
                            issue.getNumber(), reviewer);
                    outcomes.put(reviewer, ReviewerDisposition.REJECT);
                }
            }
        }
        if (outcomes.keySet().equals(reviewers)) {
            // All reviewers have expressed a conclusion
            LOG.debug("PR#{}: All reviewers have now expressed their opinion",
                    issue.getNumber());
            Set<String> labels = Util.existingLabels(pullRequest);
            if (new HashSet<>(outcomes.values()).size() == 1) {
                // And they've all reached the same conclusion
                LOG.debug("PR#{}: All reviewers have now the same opinion",
                        issue.getNumber());
                // Remove notice (if it's present)
                labels.remove(Labels.NOTICE_SPLIT_REVIEW);
                // Change state
                labels.remove(Labels.STATE_BEING_REVIEWED);
                labels.add(Labels.STATE_READY_FOR_MERGE);

            } else {
                LOG.debug("PR#{}: Reviewers have differing opinions",
                        issue.getNumber());
                Map<ReviewerDisposition, Set<String>> inverted = new LinkedHashMap<>();
                for (Map.Entry<String, ReviewerDisposition> entry : outcomes.entrySet()) {
                    inverted.computeIfAbsent(entry.getValue(), k -> new TreeSet<>()).add(entry.getKey());
                }

                // Add our own comment
                StringBuilder sb = new StringBuilder("Reviewers have differing opinions about this PR:\n");
                for (Map.Entry<ReviewerDisposition, Set<String>> entry : inverted.entrySet()) {
                    sb.append("* ").append(entry.getKey().toString().toLowerCase(Locale.ROOT)).append(":\n");
                    for (String user : entry.getValue()) {
                        sb.append("    * ").append(user).append("\n");
                    }
                }
                issue.comment(sb.toString());
                // Tag with split review
                labels.add(Labels.NOTICE_SPLIT_REVIEW);
            }
            Util.setLabels(pullRequest, labels);
        } else {
            LOG.debug("PR#{}: Not all reviewers have expressed their opinion",
                    issue.getNumber());
        }

    }
}
