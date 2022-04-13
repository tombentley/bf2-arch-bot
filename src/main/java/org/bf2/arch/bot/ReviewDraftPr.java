package org.bf2.arch.bot;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkiverse.githubapp.event.PullRequest;
import org.bf2.arch.bot.model.RecordId;
import org.bf2.arch.bot.model.RecordType;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReviewBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flow for reviewing ADR.
 *
 * Here we can automate the following:
 *
 * Has the status been updated?
 * Any banned terms (Actors/Personas)
 * ?Sentence per line?
 */
public class ReviewDraftPr {


    /**
     * When a PR that touches an ADR is marked ready for review:
     * 1. Bot adds "needs-reviewers" label
     * 2. Bot does its own code review.
     *
     * Reviewers get added manually in an arch meeting by triaging PRs with the "needs-reviewers" label
     *
     * When bot observes the addition of reviewers
     * 1. Bot removes the "needs-reviewers" label, adds the "being-reviewed" label.
     *
     * Seven days after reviewer comments have stalled, or when all reviewers have left a review:
     * Reviewers have 7 days to add their review and leave a `/accept`, `/reject` or `/defer` comment.
     *    GH approve <=> /accept,
     *    GH needs changes <=> refer
     * 1. Bot summarizes the results of the review. Adds the "ready-for-merge" label.
     *
     * PR gets merged in an arch meeting by triaging PRs with the "ready-for-merge" label.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReviewDraftPr.class);

    public void onPullRequestOpened(@PullRequest.Opened
                                     GHEventPayload.PullRequest pullRequest) throws IOException {
        if (!pullRequest.getPullRequest().isDraft()) {
            LOG.debug("ReadyForReview PR #{} opened", pullRequest.getNumber());
            files(pullRequest.getPullRequest());
        } else {
            LOG.debug("Draft PR #{} opened", pullRequest.getNumber());
        }
    }

    public void onPullRequestEdited(@PullRequest.Edited
                                    GHEventPayload.PullRequest pullRequest) throws IOException {
        if (!pullRequest.getPullRequest().isDraft()) {
            LOG.debug("ReadyForReview PR #{} edited", pullRequest.getNumber());
            files(pullRequest.getPullRequest());
        } else {
            LOG.debug("Draft PR #{} edited", pullRequest.getNumber());
        }
    }

    public void onPullRequestReadyForReview(@PullRequest.ReadyForReview
                                     GHEventPayload.PullRequest pullRequest) throws IOException {
        LOG.debug("PR #{} ReadyForReview", pullRequest.getNumber());
        files(pullRequest.getPullRequest());
    }

    public void onPullRequestComment(@IssueComment.Created
                                     GHEventPayload.IssueComment comment) throws IOException, URISyntaxException {
        if (comment.getIssue().isPullRequest()) {
            GHIssue.PullRequest pullRequest1 = comment.getIssue().getPullRequest();
            GHRepository repository = comment.getIssue().getRepository();
            int num = -1;
            String[] split = pullRequest1.getUrl().toURI().getPath().split("/");
            for (int ii = split.length - 1; ii >= 0; ii--) {
                try {
                    num = Integer.parseInt(split[ii]);
                    break;
                } catch (NumberFormatException e) {
                    // carry on
                }
            }
            GHPullRequest pullRequest = repository.getPullRequest(num);
            files(pullRequest);
        }
    }

    private void files(GHPullRequest pullRequest) throws IOException {
        var prNumber = pullRequest.getNumber();
        for (var fileDetail : pullRequest.listFiles()) {
            String repoPath = fileDetail.getFilename();
            LOG.debug("PR #{} modifies file {}",
                    prNumber, repoPath);
            for (var rt : RecordType.values()) {
                RecordId recordId = rt.recordOf(repoPath);
                if (recordId != null) {

                    // Do we consume the diff directly
                    // Or should we apply the diff, build a pre- and post- PR version of the Page
                    // and particularly the FrontMatter and see how the FrontMatter change?
                    GHRepository ourRepo = pullRequest.getBase().getRepository();
                    String defaultBranch = ourRepo.getDefaultBranch();
                    var basePage = CreateDraftRecord.getPage(ourRepo, ourRepo.getBranch(defaultBranch), repoPath);

                    GHCommitPointer head = pullRequest.getHead();
                    var theirRepo = head.getRepository();
                    var headPage = CreateDraftRecord.getPage(ourRepo, theirRepo.getBranch(head.getRef()), repoPath);

                    if (basePage.frontMatter.status.equals(headPage.frontMatter.status)) {
                        LOG.debug("PR #{} does not change the status: {}", prNumber,
                                basePage.frontMatter.status);
                    } else {
                        LOG.debug("PR #{} changes the status {} -> {}", prNumber,
                                basePage.frontMatter.status, headPage.frontMatter.status);
                    }

                    GHPullRequestReviewBuilder review = pullRequest.createReview();

                    // Validate the status
                    int statusLineNumber = 1; // TODO
                    List<String> statuses = List.of("Draft", "Accepted", "Superseded", "Rejected", "Deferred");
                    if (!statuses.contains(headPage.frontMatter.status)) {
                        review.comment("Status must be one of " + statuses, fileDetail.getFilename(), statusLineNumber);
                    }

                    // Validate the status transition (e.g. Draft -> Superseded, or Accepted -> Rejected)
                    switch (headPage.frontMatter.status) {
                        case "Deferred":
                        case "Accepted":
                        case "Rejected":
                            if (!"Draft".equals(basePage.frontMatter.status)) {
                                review.comment("Suspect state transition", fileDetail.getFilename(), statusLineNumber);
                            }
                            break;
                        case "Superseded":
                            if (!"Accepted".equals(basePage.frontMatter.status)) {
                                review.comment("Suspect state transition", fileDetail.getFilename(), statusLineNumber);
                            }
                            break;
                    }

                    // Validate that there's a git hub label for each tag
                    // Check for undefined abbrevs, and add comment
                    // Check for suspect terminology
                    // Check about sentence per line?



                }
            }
        }
    }


    public static void main(String[] args) {

        // TODO, but need to handle line numbers, or search again after the fact for the first occurrance

        Pattern acronymPattern = Pattern.compile("[A-Z0-9]{2,}");
        Set<String> definedAcronyms = new HashSet<>(Set.of("OK"));

        String text = "WOOT. This is some text with a Three Letter Abbreviation (TLA).\n" +
                "And some more stuff. It's OK to mention TLA again here.\n" +
                "But later on I might use 3LA (3 letter abbreviation), which is also fine.\n" +
                "So long as I don't use FLA and not define it. WDYT?";
        String[] words = text.split("\\W+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            System.out.println(word);
            Matcher acronymMatcher = acronymPattern.matcher(word);
            if (acronymMatcher.matches()) {
                String acronym = word;
                if (!definedAcronyms.contains(acronym)) {
                    boolean expansionFollows = true;
                    boolean expansionPreceeds = true;
                    for (int j = 0; j < acronym.length(); j++) {
                        if (i + j + 1 < words.length) {
                            String a = words[i + j + 1].substring(0, 1).toUpperCase(Locale.ROOT);
                            String b = acronym.substring(j, j + 1).toUpperCase(Locale.ROOT);
                            if (!a.equals(b)) {
                                expansionFollows = false;
                                break;
                            }
                        } else {
                            expansionFollows = false;
                            break;
                        }
                    }
                    if (!expansionFollows) {
                        for (int j = 0; j < acronym.length(); j++) {
                            if (i - acronym.length() + j >= 0) {
                                String a = words[i - acronym.length() + j].substring(0, 1).toUpperCase(Locale.ROOT);
                                String b = acronym.substring(j, j + 1).toUpperCase(Locale.ROOT);
                                if (!a.equals(b)) {
                                    expansionPreceeds = false;
                                    break;
                                }
                            } else {
                                expansionPreceeds = false;
                                break;
                            }
                        }
                    }

                    if (expansionFollows || expansionPreceeds) {
                        definedAcronyms.add(acronym);
                    } else {
                        System.err.println("Acronym undefined at this point " + acronym);
                    }
                }
            }
        }
    }


}
