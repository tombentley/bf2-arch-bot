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
package org.bf2.arch.bot.model.patch;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A collection of {@link Hunk Hunks} for a particular (unknown) file.
 *  <pre>{@code
 *  @@ -1,7 +1,7 @@
 *   ---
 *   num: 89
 *   title: "dfdff"
 *  -status: "Draft"
 *  +status: "Accepted"
 *   authors:
 *   - "tombentley"
 *   tags: []
 *  @@ -42,4 +42,4 @@ tags: []
 *   // What are the knock-on effects if this decision is accepted?
 *
 *   ## Consequences if not completed
 *  -// What are the knock-on effects if this decision is not accepted?
 *   \ No newline at end of file
 *  +// What are the knock-on effects if this decision is not accepted?
 *  }</pre>
*/
public class FilePatch {

    private final List<Hunk> hunks;

    public FilePatch(List<Hunk> hunks) {
        this.hunks = hunks;
    }

    public List<Hunk> hunks() {
        return hunks;
    }

    public static class LineMatch {
        private final Line line;
        private final Matcher matcher;
        private final int patchLineNum;

        public LineMatch(Line line, Matcher matcher, int patchLineNum) {
            this.line = line;
            this.matcher = matcher;
            this.patchLineNum = patchLineNum;
        }

        public Line line() {
            return line;
        }

        public Matcher matcher() {
            return matcher;
        }

        public int patchLineNum() {
            return patchLineNum;
        }
    }

    public Stream<LineMatch> linesMatching(EnumSet<Line.Type> type, Pattern pattern) {
        return hunks().stream()
                .flatMap(hunk -> {
                    int[] patchLineNum = new int[]{hunk.hunkStartLine() - 1};

                    return hunk.lines().stream()
                            .map(line -> {
                                patchLineNum[0]++;
                                Matcher matcher = pattern.matcher(line.line());
                                if (type.contains(line.type()) && matcher.matches()) {
                                    return new LineMatch(line, matcher, patchLineNum[0]);
                                } else {
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull);
                });
    }

    //    /**
//     * Apply a patch (as a list of hunks) to some input
//     *
//     * @param input
//     * @param hunks
//     * @return
//     */
//    static String patch(String input, Patch hunks) {
//
//        StringBuffer output = new StringBuffer();
//        int inputLineNum = 1;
//        var it = input.lines().iterator();
//        while (true) {
//            inputLineNum++;
//            if ()
//                String inputLine = it.next();
//        }
//        // TODO copy lines from input to output until we're in change of a hunk
//        // TODO then copy from the hunk:
//        // Content lines copies as is (optionally assert that they're identical to the input
//        // Ignore - lines (optionally assert that they're identical to the input
//        // Copy + lines (don't assert, obvs)
//        return null;
//    }

    /**
     * Parse a patch, returning the list of hunks.
     */
    public static FilePatch parsePatch(String patch) {
        Pattern headerPattern = Pattern.compile("^@@ -([0-9]+),([0-9]+) \\+([0-9]+),([0-9]+) @@$");
        Pattern linePattern = Pattern.compile("^([ +-])(.*)$");
        var hunks = new ArrayList<Hunk>();
        // TODO care over handling of new lines
        try {
            try (var it = new LineNumberReader(new StringReader(patch))) {
                String line = it.readLine();
                while (line != null) {
                    Matcher headerMatcher = headerPattern.matcher(line);
                    if (!headerMatcher.matches()) {
                        throw new IllegalStateException();
                    }
                    // See https://docs.github.com/en/rest/reference/pulls#create-a-review-comment-for-a-pull-request
                    int hunkStartLine = it.getLineNumber();
                    var lines = new ArrayList<Line>();
                    line = it.readLine();
                    while (line != null) {
                        Matcher lineMatcher = linePattern.matcher(line);
                        if (lineMatcher.matches()) {
                            Line.Type type;
                            switch (lineMatcher.group(1)) {
                                case " ":
                                    type = Line.Type.CONTEXT;
                                    break;
                                case "+":
                                    type = Line.Type.ADD;
                                    break;
                                case "-":
                                    type = Line.Type.REMOVE;
                                    break;
                                default:
                                    throw new IllegalStateException();
                            }
                            Line l = new Line(type, lineMatcher.group(2));
                            lines.add(l);
                        } else {
                            break;
                        }
                        line = it.readLine();
                    }
                    var hunk = new Hunk(hunkStartLine,
                            Integer.parseInt(headerMatcher.group(1)),
                            Integer.parseInt(headerMatcher.group(2)),
                            Integer.parseInt(headerMatcher.group(3)),
                            Integer.parseInt(headerMatcher.group(4)),
                            lines);
                    hunks.add(hunk);
                }
            }
            return new FilePatch(hunks);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
