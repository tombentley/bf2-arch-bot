package org.bf2.arch.bot;

import java.util.EnumSet;
import java.util.List;

import org.bf2.arch.bot.model.record.RecordType;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
class ArchReviewStateMachineFlowTest {

    @Test
    public void testTouchesRecord() {
        GHPullRequest pullRequest = mock(GHPullRequest.class);
        when(pullRequest.getNumber()).thenReturn(42);

        var file1 = mock(GHPullRequestFileDetail.class);
        when(file1.getFilename()).thenReturn("_adr/12/index.adoc");
        var file2 = mock(GHPullRequestFileDetail.class);
        when(file2.getFilename()).thenReturn("_adr/10/index.adoc");
        var file3 = mock(GHPullRequestFileDetail.class);
        when(file3.getFilename()).thenReturn("some/other/file.adoc");

        PagedIterable<GHPullRequestFileDetail> pagedIterable = mock(PagedIterable.class);
        when(pagedIterable.iterator()).thenAnswer(invocation -> {
            PagedIterator<GHPullRequestFileDetail> iterator = mock(PagedIterator.class);
            var it = List.of(file1, file2, file3).iterator();
            when(iterator.hasNext()).thenAnswer(i -> it.hasNext());
            when(iterator.next()).thenAnswer(i -> it.next());
            return iterator;
        });
        when(pullRequest.listFiles()).thenReturn(pagedIterable);

        EnumSet<RecordType> recordTypes = new ArchReviewStateMachineFlow().touchesRecord(pullRequest);

        assertEquals(EnumSet.of(RecordType.ADR), recordTypes);
    }
}