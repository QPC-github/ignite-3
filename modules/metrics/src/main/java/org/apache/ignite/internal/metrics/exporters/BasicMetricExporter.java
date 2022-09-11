/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.metrics.exporters;

import java.util.Map;
import org.apache.ignite.internal.metrics.MetricProvider;
import org.apache.ignite.internal.metrics.MetricSet;
import org.apache.ignite.lang.IgniteBiTuple;

/**
 * Base class for new metrics exporters implementations.
 */
public abstract class BasicMetricExporter implements MetricExporter {
    /** Metrics provider. */
    private MetricProvider metricsProvider;

    /** {@inheritDoc} */
    @Override
    public final void init(MetricProvider metricProvider) {
        this.metricsProvider = metricProvider;
    }

    /** {@inheritDoc} */
    @Override
    public final String name() {
        return getClass().getCanonicalName();
    }

    /**
     * Returns a map of (metricSetName -> metricSet) pairs with available metrics.
     *
     * @return map of metrics
     */
    protected final IgniteBiTuple<Map<String, MetricSet>, Long> metrics() {
        return metricsProvider.metrics();
    }
}