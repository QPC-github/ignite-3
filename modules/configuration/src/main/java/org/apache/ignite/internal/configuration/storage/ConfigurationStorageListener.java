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

package org.apache.ignite.internal.configuration.storage;

import java.util.concurrent.CompletableFuture;

/**
 * Configuration storage listener for changes.
 */
public interface ConfigurationStorageListener {
    /**
     * Method called when entries in storage change.
     *
     * @param changedEntries Changed entries, key-value pairs and new version of the storage.
     * @return Completable future that signifies the completion of all custom user listeners execution.
     */
    CompletableFuture<Void> onEntriesChanged(Data changedEntries);

    /**
     * Method called when a revision of the underlying storage changed, but no entries were changed.
     *
     * @param newRevision New storage revision.
     * @return Completable future that signifies the completion of all custom user listeners execution.
     */
    CompletableFuture<Void> onRevisionUpdated(long newRevision);
}
