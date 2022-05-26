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

import java.util.Set;

/**
 * The bot uses families/groups of labels.
 */
public class Labels {
    /**
     * The tag prefix is used by {@link CreateDraftFlow} to create Draft records
     * with frontmatter tags matching the {@code tag:} labels on the issue.
     */
    public static final String PREFIX_TAG = "tag: ";

    /**
     * The type prefix is used by the {@link ArchReviewStateMachineFlow} to partition PRs
     * into those which follow the state machine (labelled {@code type: content}), and
     * those which do not.
     */
    public static final String PREFIX_TYPE = "type: ";
    public static final String TYPE_ADR = PREFIX_TYPE + "adr";
    public static final String TYPE_PADR = PREFIX_TYPE + "padr";
    public static final String TYPE_AP = PREFIX_TYPE + "ap";
    public static final String TYPE_INFRA = PREFIX_TYPE + "infra";

    public static Set<String> typeLabels() {
        return Set.of(TYPE_ADR, TYPE_AP, TYPE_PADR, TYPE_INFRA);
    }

    /**
     * The state prefix is used by the {@link ArchReviewStateMachineFlow} to
     * identify which state a {@code type: content}-labelled PR is in.
     */
    public static final String PREFIX_STATE = "state: ";
    public static final String STATE_NEEDS_REVIEWERS = PREFIX_STATE + "needs-reviewers";
    public static final String STATE_BEING_REVIEWED = PREFIX_STATE + "being-reviewed";
    public static final String STATE_READY_FOR_MERGE = PREFIX_STATE + "ready-for-merge";

    public static Set<String> stateLabels() {
        return Set.of(STATE_NEEDS_REVIEWERS, STATE_BEING_REVIEWED, STATE_READY_FOR_MERGE);
    }

    /**
     * The notice prefix is used by the{@link ArchReviewStateMachineFlow} to
     * identify PRs which need the attention of a human.
     */
    public static final String PREFIX_NOTICE = "notice: ";
    /** Label for PRs where the discussion seems to have stopped */
    public static final String NOTICE_STALLED_DISCUSSION = PREFIX_NOTICE + "stalled-discussion";
    /** Label for PRs where reviewers haven't reached a consensus */
    public static final String NOTICE_SPLIT_REVIEW = PREFIX_NOTICE + "split-review";
    /**
     * Label for PRs where, even though discussion seems to be continuing,
     * the PR has been opened for a while and a synchrnous discussion might be needed
     * to bring about an early conclusion
     */
    public static final String NOTICE_OVERDUE = PREFIX_NOTICE + "overdue";

    public static Set<String> noticeLabels() {
        return Set.of(NOTICE_OVERDUE, NOTICE_SPLIT_REVIEW, NOTICE_STALLED_DISCUSSION);
    }

}
