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

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkiverse.githubapp.testing.GitHubAppTesting;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.mockito.Mockito;

@QuarkusTest
@GitHubAppTest
class CreateCommentTest {
    @Test
    void testIssueOpened() throws IOException {
        GitHubAppTesting.when()
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
