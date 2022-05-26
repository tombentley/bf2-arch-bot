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

/**
 * A line in a patch
 */
public class Line {

    public enum Type {
        CONTEXT(' '),
        ADD('+'),
        REMOVE('-');
        private final char prefix;

        private Type(char prefix) {
            this.prefix = prefix;
        }
    }

    private final Type type;
    private final String line;

    public Line(Type type, String line) {
        this.type = type;
        this.line = line;
    }

    public Type type() {
        return type;
    }

    public String line() {
        return line;
    }

    @Override
    public String toString() {
        return type.prefix + line;
    }
}
