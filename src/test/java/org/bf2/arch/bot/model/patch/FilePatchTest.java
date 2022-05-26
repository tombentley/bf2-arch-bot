package org.bf2.arch.bot.model.patch;

import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class FilePatchTest {

    @Test
    public void testSingleHunk() {
        FilePatch filePatch = FilePatch.parsePatch(
                "@@ -1,7 +2,7 @@\n" +
                        " ---\n" +
                        " num: 89\n" +
                        " title: \"dfdff\"\n" +
                        "-status: \"Draft\"\n" +
                        "+status: \"Accepted\"\n" +
                        " authors:\n" +
                        " - \"tombentley\"\n" +
                        " tags: []\n");
        assertEquals(1, filePatch.hunks().size());
        Hunk hunk = filePatch.hunks().get(0);
        assertEquals(1, hunk.hunkStartLine());
        assertEquals(1, hunk.oldStartLine());
        assertEquals(7, hunk.oldLength());
        assertEquals(2, hunk.newStartLine());
        assertEquals(7, hunk.newLength());
        assertEquals(8, hunk.lines().size());
        assertEquals(Line.Type.CONTEXT, hunk.lines().get(0).type());
        assertEquals("---", hunk.lines().get(0).line());
        assertEquals(Line.Type.REMOVE, hunk.lines().get(3).type());
        assertEquals("status: \"Draft\"", hunk.lines().get(3).line());
        assertEquals(Line.Type.ADD, hunk.lines().get(4).type());
        assertEquals("status: \"Accepted\"", hunk.lines().get(4).line());
    }

    @Test
    public void testTwoHunk() {
        FilePatch filePatch = FilePatch.parsePatch(
                "@@ -1,7 +2,7 @@\n" +
                        " ---\n" +
                        " num: 89\n" +
                        " title: \"dfdff\"\n" +
                        "-status: \"Draft\"\n" +
                        "+status: \"Accepted\"\n" +
                        " authors:\n" +
                        " - \"tombentley\"\n" +
                        " tags: []\n" +
                        "@@ -101,7 +102,7 @@\n" +
                        " ---\n" +
                        " num: 89\n" +
                        " title: \"dfdff\"\n" +
                        "-status: \"Draft\"\n" +
                        "+status: \"Accepted\"\n" +
                        " authors:\n" +
                        " - \"tombentley\"\n" +
                        " tags: []\n");
        assertEquals(2, filePatch.hunks().size());
        Hunk hunk = filePatch.hunks().get(1);
        assertEquals(10, hunk.hunkStartLine());
        assertEquals(101, hunk.oldStartLine());
        assertEquals(7, hunk.oldLength());
        assertEquals(102, hunk.newStartLine());
        assertEquals(7, hunk.newLength());
        assertEquals(8, hunk.lines().size());
        assertEquals(Line.Type.CONTEXT, hunk.lines().get(0).type());
        assertEquals("---", hunk.lines().get(0).line());
        assertEquals(Line.Type.REMOVE, hunk.lines().get(3).type());
        assertEquals("status: \"Draft\"", hunk.lines().get(3).line());
        assertEquals(Line.Type.ADD, hunk.lines().get(4).type());
        assertEquals("status: \"Accepted\"", hunk.lines().get(4).line());
    }

    @Test
    public void testLinesMatching() {
        FilePatch filePatch = FilePatch.parsePatch(
                "@@ -1,7 +2,7 @@\n" +
                        " ---\n" +
                        " num: 89\n" +
                        " title: \"dfdff\"\n" +
                        "-status: \"Draft\"\n" +
                        "+status: \"Accepted\"\n" +
                        " authors:\n" +
                        " - \"tombentley\"\n" +
                        " tags: []\n" +
                        "@@ -101,7 +102,7 @@\n" +
                        " ---\n" +
                        " num: 89\n" +
                        " title: \"dfdff\"\n" +
                        "-status: \"Draft\"\n" +
                        "+status: \"Foo\"\n" +
                        " authors:\n" +
                        " - \"tombentley\"\n" +
                        " tags: []\n");
        var list = filePatch.linesMatching(EnumSet.of(Line.Type.ADD),
                Pattern.compile("^status: .*$")).collect(Collectors.toList());
        assertEquals(2, list.size());
        assertEquals(5, list.get(0).patchLineNum());
        assertEquals("status: \"Accepted\"", list.get(0).line().line());

        assertEquals(14, list.get(1).patchLineNum());
        assertEquals("status: \"Foo\"", list.get(1).line().line());
    }

}