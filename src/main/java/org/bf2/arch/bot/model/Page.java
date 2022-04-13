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
package org.bf2.arch.bot.model;

import java.io.IOException;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class Page {
    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    public final FrontMatter frontMatter;
    public final String bodyContent;

    public Page(FrontMatter frontMatter, String bodyContent) {
        this.frontMatter = frontMatter;
        this.bodyContent = bodyContent;
    }

    public static Page fromContent(String content) throws IOException {
        String[] parts = content.split("---+", 3);
        String frontmatter = parts[1];
        String bodyContent = parts[2];
        var fm = YAML_MAPPER.readValue(frontmatter, FrontMatter.class);
        return new Page(fm, bodyContent);
    }

    public String toContentString() throws IOException {
        return YAML_MAPPER.writeValueAsString(frontMatter) + "---" + bodyContent;
    }
}
