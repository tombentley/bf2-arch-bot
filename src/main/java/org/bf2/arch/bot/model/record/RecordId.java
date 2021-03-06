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

import java.util.Objects;

/**
 * The id for a record, consistenting of a {@linkplain RecordType record type} and an id number.
 */
public class RecordId {
    private final RecordType recordType;
    private final int num;

    public RecordId(RecordType recordType, int num) {
        this.recordType = recordType;
        this.num = num;
    }

    public RecordType recordType() {
        return recordType;
    }

    public int num() {
        return num;
    }

    public String repoPath() {
        return recordType.path(num);
    }

    public String publishedUrl(String baseUrl) {
        return String.format("%s/%s/%d/", baseUrl, recordType.publishedPath, num);
    }

    public String toString() {
        return recordType + "-" + num;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordId that = (RecordId) o;
        return num == that.num && recordType == that.recordType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordType, num);
    }

}
