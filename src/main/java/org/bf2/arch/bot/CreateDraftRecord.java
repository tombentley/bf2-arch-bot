package org.bf2.arch.bot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.githubapp.event.IssueComment;
import org.bf2.arch.bot.model.Page;
import org.bf2.arch.bot.model.RecordId;
import org.bf2.arch.bot.model.RecordType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flow for creating a Draft record (AP, ADR, PADR), and possibly superseding an existing record.
 *
 * Precondition: A user has created an issue identifying the need for an ADR (or AP etc).
 *
 * <h4>Normal creation</h4>
 * <ol>
 * <li>An architect comments `/create-adr` on the issue
 * <li>The bot allocates a unique id and opens a PR using the issue description for content
 * <li>The bot merges the PR
 * </ol>
 *
 * <h4>Supersedure</h4>
 * <ol>
 * <li>An architect comments `/supersede adr 12` on the issue
 * <li>The bot allocates a unique id and opens a PR using the issue description for content.
 * The PR also changes the superseded record's status to Superseded.
 * <li>The bot merges the PR
 * </ol>
 *
 * Post condition: An ADR in the draft state exists.
 * The user can then edit that ADR in their own branch.
 */
public class CreateDraftRecord {

    public static final Pattern CMD_CREATE = Pattern.compile("/create +(?<recordType>p?adr|ap)", Pattern.CASE_INSENSITIVE);
    public static final Pattern CMD_SUPERSEDE = Pattern.compile("/supersede +(?<recordType>p?adr|ap) +(?<num>[0-9]+)", Pattern.CASE_INSENSITIVE);

    private static final Logger LOG = LoggerFactory.getLogger(CreateDraftRecord.class);

    ArchBotConfig config = new ArchBotConfig(new ArchBotConfig.TeamRef("bf2fc6cc711aee1a0c2a",
            "architects"));

    /**
     * Creates a PR for a Draft ADR when an issue comment has {@code /create-adr} (or ap, or padr),
     * or {@code /supersede adr 123}.
     * @param commentPayload The payload
     * @param client The client
     * @throws IOException From github, or parsing pages
     */
    public void onIssueComment(
            @IssueComment.Created @IssueComment.Edited GHEventPayload.IssueComment commentPayload,
            GitHub client) throws IOException {
        if (config == null) {
            throw new IllegalStateException("Repo is missing config file");
        }
        boolean authorized = isAuthorized(commentPayload, client, config.adrCreationApprovers);
        String body = commentPayload.getComment().getBody().trim();
        Matcher createMatcher = CMD_CREATE.matcher(body);
        if (createMatcher.matches()
                && authorized) {
            var recordType = RecordType.valueOf(createMatcher.group("recordType").toUpperCase(Locale.ROOT));
            createDraft(commentPayload, recordType, -1);
        } else {
            Matcher supersedureMatcher = CMD_SUPERSEDE.matcher(body);
            if (supersedureMatcher.matches()
                    && authorized) {
                var recordType = RecordType.valueOf(supersedureMatcher.group("recordType").toUpperCase(Locale.ROOT));
                var supersedesRecord = Integer.parseInt(supersedureMatcher.group("num"));
                createDraft(commentPayload, recordType, supersedesRecord);
            } else {
                LOG.debug("Ignoring message on issue #{}: {}", commentPayload.getIssue().getNumber(), body);
            }
        }

        // TODO support /help
        // TODO should we only respond on open issues?
    }

    private boolean isAuthorized(GHEventPayload.IssueComment commentPayload, GitHub client, ArchBotConfig.TeamRef adrCreationApprovers) throws IOException {
        // TODO
//        GHOrganization org = client.getOrganization(adrCreationApprovers.org);
//        GHTeam team = org.getTeamByName(adrCreationApprovers.team);
//        boolean authorized = commentPayload.getComment().getUser().isMemberOf(team);
        return true;
    }

    private void createDraft(GHEventPayload.IssueComment commentPayload,
                             RecordType recordType,
                             int supersedesRecordNum) throws IOException {
        var issue = commentPayload.getIssue();
        try {
            var repo = issue.getRepository();
            LOG.debug("Creating {} in {}", recordType, repo);
            var defaultBranchName = repo.getDefaultBranch();
            var defaultBranch = repo.getBranch(defaultBranchName);
            LOG.debug("Using branch {} to find current max ID", defaultBranch.getName());
            var defaultBranchSha = defaultBranch.getSHA1();
            LOG.debug("Branch {} tip is {}", defaultBranch.getName(), defaultBranchSha);
            // Allocate a new ADR id
            var draftRecord = new RecordId(recordType, allocateId(repo, defaultBranchSha, recordType));

            var tree = repo.createTree()
                    .baseTree(defaultBranchSha);

            // generate content from template
            var draftRecordContent = draftContent(repo, defaultBranch,
                    draftRecord,
                    issue.getTitle(),
                    recordAuthors(issue).collect(Collectors.toList()),
                    recordTags(issue),
                    -1);
            tree.add(draftRecord.repoPath(), draftRecordContent.toContentString(), false);

            // update the supersede record
            if (supersedesRecordNum > 0) {
                // update the superseded record content
                var supersededRecord = new RecordId(recordType, supersedesRecordNum);
                // TODO handle the case where the superseded record doesn't exist
                var supersededPage = supersededContent(repo, defaultBranch, supersededRecord, draftRecord.num());
                tree.add(supersededRecord.repoPath(), supersededPage.toContentString(), false);
            }

            var commitMessage = String.format("%s: Create draft\nFixes #%d", draftRecord, issue.getNumber());
            var branchRef = createCommit(draftRecord, repo, defaultBranchSha, commitMessage, tree.create());

            // Open a PR
            var pr = openPullRequest(repo, defaultBranchName, draftRecord, commitMessage, branchRef);

            // Merge it
            pr.merge(commitMessage, null, GHPullRequest.MergeMethod.REBASE);

            // Comment on the issue with instructions
            issue.comment(String.format(
                    "Closing following creation of [%s](%s)\n" +
                            "%s, please write your content in %s and open a PR for %s acceptance.",
                    draftRecord, draftRecord.publishedUrl(),
                    recordAuthors(issue).map(login -> "@" + login).collect(Collectors.joining(", ")),
                    draftRecord.repoPath(), draftRecord.recordType()));

            // Close the issue
            issue.close();
        } catch (BotBusinessError e) {
            issue.comment(e.getMessage());
        }
    }

    @NotNull
    private static Page supersededContent(GHRepository repo,
                                   GHBranch defaultBranch,
                                   RecordId supersededRecord,
                                   int supersededBy) throws IOException {
        var supersededPage = getPage(repo, defaultBranch, supersededRecord.repoPath());
        if (supersededPage == null) {
            throw new BotBusinessError(String.format("There is no %s with number %d", supersededRecord.recordType(), supersededRecord.num()));
        }
        supersededPage.frontMatter.status = "Superseded";
        supersededPage.frontMatter.supersededBy = supersededBy;
        return supersededPage;
    }

    @NotNull
    private List<String> recordTags(GHIssue issue) {
        return issue.getLabels().stream()
                .filter(label -> label.getName().startsWith("tag: "))
                .map(label -> label.getName().substring("tag: ".length()))
                .collect(Collectors.toList());
    }

    @NotNull
    private Stream<String> recordAuthors(GHIssue issue) throws IOException {
        Stream<String> authors;
        if (issue.getAssignees().isEmpty()) {
            authors = Stream.of(issue.getUser().getLogin());
        } else {
            authors = issue.getAssignees().stream()
                    .map(GHPerson::getLogin);
        }
        return authors;
    }

    private GHPullRequest openPullRequest(GHRepository repo,
                                          String defaultBranchName,
                                          RecordId record,
                                          String commitMessage,
                                          String branchRef) throws IOException {
        var markdown = String.format("Create %s in draft state", record);
        var title = commitMessage;
        return repo.createPullRequest(title,
                branchRef,
                defaultBranchName,
                markdown);
    }


    private String createCommit(RecordId record,
                                GHRepository repo,
                                String defaultBranchSha,
                                String commitMessage,
                                GHTree tree) throws IOException {
        GHCommit commit = repo.createCommit()
                .parent(defaultBranchSha)
                .message(commitMessage)
                .tree(tree.getSha()).create();

        var refName = String.format("refs/heads/create-%s", record);
        repo.createRef(refName, commit.getSHA1());
        return refName;
    }

    @NotNull
    private Page draftContent(GHRepository repo,
                                GHBranch defaultBranch,
                                RecordId record,
                                String title,
                                List<String> authors,
                                List<String> tags,
                                int superseded) throws IOException {
        var templateRepoPath = record.recordType().path(0);
        return renderTemplate(record,
                title,
                authors,
                tags,
                superseded,
                getPage(repo, defaultBranch, templateRepoPath));
    }

    @NotNull
    static Page getPage(GHRepository repo, GHBranch defaultBranch, String repoPath) throws IOException {
        String content = getContent(repo, defaultBranch, repoPath);
        if (content == null) return null;
        return Page.fromContent(content);
    }

    @Nullable
    static String getContent(GHRepository repo, GHBranch defaultBranch, String repoPath) throws IOException {
        GHContent fileContent = repo.getFileContent(repoPath, defaultBranch.getSHA1());
        if (fileContent == null) {
            return null;
        }
        return new String(fileContent.read().readAllBytes(), StandardCharsets.UTF_8);
    }

    @NotNull
    static Page renderTemplate(RecordId record,
                                 String title,
                                 List<String> authors,
                                 List<String> tags,
                                 int supersededBy,
                                 Page page) {
        page.frontMatter.num = record.num();
        page.frontMatter.title = title;
        page.frontMatter.status = "Draft";
        page.frontMatter.authors = authors;
        page.frontMatter.tags = tags;
        if (supersededBy > 0) {
            page.frontMatter.supersededBy = supersededBy;
        }
        return page;
    }

    int allocateId(GHRepository repo, String commitSha, RecordType recordType) throws IOException {
        var collect = collect(repo.getCommit(commitSha).getTree(), recordType);
        var maxId = collect.stream().map(entry -> {
            String path = entry.getPath();
            LOG.debug("Path: {}", path);
            try {
                return Integer.parseInt(path);
            } catch (NumberFormatException e) {
                return -1;
            }
        }).max(Integer::compareTo);

        LOG.debug("Max existing {} id {}", recordType, maxId);
        var nextId = maxId.orElse(1) + 1;
        LOG.debug("Next {} id {}", recordType, nextId);
        return nextId;
    }

    List<GHTreeEntry> collect(GHTree root, RecordType recordType) throws IOException {
        var recordEntries = root.getEntry(recordType.dir);
        LOG.debug("{} entry: {}", recordType.dir, recordEntries);
        return recordEntries.asTree().getTree();
    }
}
