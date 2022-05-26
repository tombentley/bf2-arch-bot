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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the frontmatter of records
 */
public class RecordFrontMatter {

    //        num: 0 # allocate an id when the draft is created
    //        title: ADR template
    //        status: "Draft" # One of Draft, Accepted, Rejected
    //        authors:
    //        - "" # One item for each author, as github id or "firstname lastname"
    //        tags:
    //        - "" # e.g. kafka, connectors, registry
    //        applies_padrs: # What PADRs does this ADR apply?
    //        applies_patterns: # What APs does this ADR apply?
    //        ---

    @JsonProperty
    public int num;
    @JsonProperty
    public String title;
    @JsonProperty
    public String status;
    @JsonProperty
    public List<String> authors;
    @JsonProperty
    public List<String> tags;
    @JsonProperty("superseded_by")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer supersededBy;

    @JsonAnySetter
    @JsonAnyGetter
    public Map<String, Object> any;
}
