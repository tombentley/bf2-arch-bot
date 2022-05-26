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
package org.bf2.arch.bot.model.record;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum RecordType {
    ADR("_adr", "adr"),
    AP("_ap", "ap"),
    PADR("_padr", "padr");
    public final String repoDir;

    public final String publishedPath;

    RecordType(String repoDir, String publishedPath) {
        this.repoDir = repoDir;
        this.publishedPath = publishedPath;
    }

    public String path(int recordId) {
        return String.format("%s/%d/index.adoc", repoDir, recordId);
    }

    public RecordId recordOf(String repoPath) {
        Pattern p = Pattern.compile(repoDir + "/(?<num>[0-9]+)/index.adoc");
        Matcher matcher = p.matcher(repoPath);
        if (matcher.matches()) {
            return new RecordId(this, Integer.parseInt(matcher.group("num")));
        } else {
            return null;
        }
    }
}
