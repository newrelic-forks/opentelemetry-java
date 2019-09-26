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

package io.opentelemetry.sdk.trace.export;

import io.opentelemetry.internal.Utils;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.trace.TraceFlags;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the {@link SpanProcessor} that converts the {@link ReadableSpan} to {@link
 * io.opentelemetry.proto.trace.v1.Span} and passes it to the configured exporter.
 *
 * <p>Only spans that are sampled are converted, {@link TraceFlags#isSampled()} must return {@code
 * true}.
 */
public final class SimpleSampledSpansProcessor implements SpanProcessor {
  private static final Logger logger =
      Logger.getLogger(SimpleSampledSpansProcessor.class.getName());

  private final SpanExporter spanExporter;

  private SimpleSampledSpansProcessor(SpanExporter spanExporter) {
    this.spanExporter = Utils.checkNotNull(spanExporter, "spanExporter");
  }

  @Override
  public void onStart(ReadableSpan span) {
    // Do nothing.
  }

  @Override
  public void onEnd(ReadableSpan span) {
    if (!span.getSpanContext().getTraceFlags().isSampled()) {
      return;
    }
    try {
      spanExporter.export(Collections.singletonList(makeSpanData(span)));
    } catch (Throwable e) {
      logger.log(Level.WARNING, "Exception thrown by the export.", e);
    }
  }

  private SpanData makeSpanData(ReadableSpan readableSpan) {
    throw new UnsupportedOperationException("implement met!!!");
  }

  @Override
  public void shutdown() {
    spanExporter.shutdown();
  }

  /**
   * Returns a new Builder for {@link SimpleSampledSpansProcessor}.
   *
   * @param spanExporter the {@code SpanExporter} to where the sampled Spans are pushed.
   * @return a new {@link SimpleSampledSpansProcessor}.
   * @throws NullPointerException if the {@code spanExporter} is {@code null}.
   */
  public static Builder newBuilder(SpanExporter spanExporter) {
    return new Builder(spanExporter);
  }

  /** Builder class for {@link SimpleSampledSpansProcessor}. */
  public static final class Builder {
    private final SpanExporter spanExporter;

    private Builder(SpanExporter spanExporter) {
      this.spanExporter = Utils.checkNotNull(spanExporter, "spanExporter");
    }

    // TODO: Add metrics for total exported spans.
    // TODO: Consider to add support for constant Attributes and/or Resource.

    /**
     * Returns a new {@link SimpleSampledSpansProcessor} that converts spans to proto and forwards
     * them to the given {@code spanExporter}.
     *
     * @return a new {@link SimpleSampledSpansProcessor}.
     * @throws NullPointerException if the {@code spanExporter} is {@code null}.
     */
    public SimpleSampledSpansProcessor build() {
      return new SimpleSampledSpansProcessor(spanExporter);
    }
  }
}
