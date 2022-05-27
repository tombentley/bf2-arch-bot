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
import java.util.List;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.GitHubAppTesting;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@GitHubAppTest
class CreateCommentTest {
    @Test
    @Disabled
    void testIssueOpened() throws IOException {
        GitHubAppTesting.given().github(mocks -> {
                    mocks.configFileFromClasspath(Util.CONFIG_REPO_PATH, "/config.yaml");
                    GHRepository repository = mocks.repository("app-services-architecture");
                    when(repository.getDefaultBranch()).thenReturn("main");
                    var defaultBranch = mock(GHBranch.class);
                    when(defaultBranch.getName()).thenReturn("main");
                    when(defaultBranch.getSHA1()).thenReturn("1234");

                    var entries = List.of(mock(GHTreeEntry.class));

                    var tree2 = mock(GHTree.class);
                    when(tree2.getTree()).thenReturn(entries);

                    var entry = mock(GHTreeEntry.class);
                    when(entry.asTree()).thenReturn(tree2);

                    var tree = mock(GHTree.class);
                    when(tree.getEntry("_adr")).thenReturn(entry);

                    var commit = mock(GHCommit.class);
                    when(commit.getTree()).thenReturn(tree);
                    when(repository.getBranch("main")).thenReturn(defaultBranch);
                    when(repository.getCommit("1234")).thenReturn(commit);
        }).when()
                .payloadFromClasspath("/1-user-comment-issue.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
//                    Mockito.verify(mocks.pullRequest(949172137))
//                            .getNumber();
                });
        GitHubAppTesting.when()
                .payloadFromClasspath("/2-bot-open-pr.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    var n = mocks.issue(51).getNumber();
                    var body = mocks.issueComment(1139495387).getBody();
                    var y = mocks.pullRequest(949172137).getNumber();
                    var x = mocks.pullRequest(51).getNumber();
                    Mockito.verify(mocks.pullRequest(949172137))
                            .setTitle("ADR-113: Create draft\nFixes #50");
                });
    }
}
