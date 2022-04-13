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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of Hunks
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
public class Patch {

    private final Map<String, Hunk> hunks;

    public Patch(Map<String, Hunk> hunks) {
        this.hunks = hunks;
    }

    /**
     * Apply a patch (as a list of hunks) to some input
     *
     * @param input
     * @param hunks
     * @return
     */
    static String patch(String input, Patch hunks) {

        StringBuffer output = new StringBuffer();
        int inputLineNum = 1;
        var it = input.lines().iterator();
        while (true) {
            inputLineNum++;
            if ()
                String inputLine = it.next();
        }
        // TODO copy lines from input to output until we're in change of a hunk
        // TODO then copy from the hunk:
        // Content lines copies as is (optionally assert that they're identical to the input
        // Ignore - lines (optionally assert that they're identical to the input
        // Copy + lines (don't assert, obvs)
        return null;
    }

    /**
     * Parse a patch, returning the list of hunks.
     */
    static Patch parsePatch(String patch) {
        Pattern headerPattern = Pattern.compile("^@@ -([0-9]+),([0-9]+) +([0-9]+),([0-9]+), @@$");
        Pattern linePattern = Pattern.compile("^([ +-])(.*)$");
        var hunks = new ArrayList<Hunk>();
        // TODO care over handling of new lines
        var it = patch.lines().iterator();
        while (it.hasNext()) {
            var line = it.next();
            Matcher headerMatcher = headerPattern.matcher(line);
            if (!headerMatcher.matches()) {
                throw new IllegalStateException();
            }

            var lines = new ArrayList<Line>();
            while (it.hasNext()) {
                line = it.next();
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
            }
            var hunk = new Hunk(Integer.parseInt(headerMatcher.group(1)),
                    Integer.parseInt(headerMatcher.group(2)),
                    Integer.parseInt(headerMatcher.group(3)),
                    Integer.parseInt(headerMatcher.group(4)),
                    lines);
            hunks.add(hunk);
        }
        return hunks;
    }
}
