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
import java.util.Set;
import java.util.stream.Collectors;

import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public class Prereq {

    // TODO is this really needed? It seems github will just auto-create labels when used by bots
    // So only value this adds is just using a consistent color and description?
    public void createLabels(GitHub client) throws IOException {
        GHRepository repository = client.getRepository("...");
        Set<String> existingLabels = repository.listLabels().toList().stream().map(GHLabel::getName).collect(Collectors.toSet());
        for (String label : Labels.typeLabels()) {
            if (!existingLabels.contains(label)) {
                repository.createLabel(label, "#D99A91", "PR that touch " + label);
            }
        }
        for (String label : Labels.stateLabels()) {
            if (!existingLabels.contains(label)) {
                repository.createLabel(label, "#584CB5", "PRs " + label.replace("-", " "));
            }
        }
        for (String label : Labels.noticeLabels()) {
            if (!existingLabels.contains(label)) {
                repository.createLabel(label, "#D93F0B", "PRs " + label.replace("-", " "));
            }
        }
    }
}
