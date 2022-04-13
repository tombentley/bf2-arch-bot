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
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * A hunk of a patch
 */
public class Hunk {

    final int oldStartLine;
    final int oldLength;
    final int newStartLine;
    final int newLength;
    final List<Line> changes;

    public Hunk(int oldStartLine, int oldLength, int newStartLine, int newLength, List<Line> changes) {
        this.oldStartLine = oldStartLine;
        this.oldLength = oldLength;
        this.newStartLine = newStartLine;
        this.newLength = newLength;
        this.changes = changes;
    }

    @Override
    public String toString() {
        return String.format("@@ -%d,%d +%d,%d @@", oldLength, oldLength, newStartLine, newLength);
    }


}
