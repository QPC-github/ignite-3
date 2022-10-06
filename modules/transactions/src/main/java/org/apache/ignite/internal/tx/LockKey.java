/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.tx;

import java.util.UUID;

/** Lock key. */
public class LockKey {
    /** Context identificator. */
    private final UUID contextId;

    /** Entity to lock. */
    private final Object key;

    public LockKey(Object key) {
        this(null, key);
    }

    public LockKey(UUID contextId, Object key) {
        this.contextId = contextId;
        this.key = key;
    }

    /**
     * Returns context identificator.
     *
     * @return Context identificator.
     */
    public UUID contextId() {
        return contextId;
    }

    /**
     * Returns key.
     *
     * @return Key.
     */
    public Object key() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LockKey lockKey = (LockKey) o;

        if (contextId != null ? !contextId.equals(lockKey.contextId) : lockKey.contextId != null) {
            return false;
        }
        return key != null ? key.equals(lockKey.key) : lockKey.key == null;
    }

    @Override
    public int hashCode() {
        int result = contextId != null ? contextId.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}