/*
 * Copyright 2019, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.extensions.metrics.runtime;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.common.Labels;
import io.opentelemetry.metrics.AsynchronousInstrument;
import io.opentelemetry.metrics.AsynchronousInstrument.LongResult;
import io.opentelemetry.metrics.LongSumObserver;
import io.opentelemetry.metrics.Meter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports metrics about JVM garbage collectors.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * new GarbageCollector().exportAll();
 * }</pre>
 *
 * <p>Example metrics being exported:
 *
 * <pre>
 *   jvm_gc_collection{gc="PS1"} 6.7
 * </pre>
 */
public final class GarbageCollector {
  private static final String GC_LABEL_KEY = "gc";

  private final List<GarbageCollectorMXBean> garbageCollectors;
  private final Meter meter;

  /** Constructs a new module that is capable to export metrics about "jvm_gc". */
  public GarbageCollector() {
    this.garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();
    this.meter = OpenTelemetry.getMeter("jvm_gc");
  }

  /** Export all metrics generated by this module. */
  public void exportAll() {
    final LongSumObserver gcMetric =
        meter
            .longSumObserverBuilder("collection")
            .setDescription("Time spent in a given JVM garbage collector in milliseconds.")
            .setUnit("ms")
            .build();
    final List<Labels> labelSets = new ArrayList<>(garbageCollectors.size());
    for (final GarbageCollectorMXBean gc : garbageCollectors) {
      labelSets.add(Labels.of(GC_LABEL_KEY, gc.getName()));
    }

    gcMetric.setCallback(
        new AsynchronousInstrument.Callback<LongResult>() {
          @Override
          public void update(LongResult resultLongObserver) {
            for (int i = 0; i < garbageCollectors.size(); i++) {
              resultLongObserver.observe(
                  garbageCollectors.get(i).getCollectionTime(), labelSets.get(i));
            }
          }
        });
  }
}
