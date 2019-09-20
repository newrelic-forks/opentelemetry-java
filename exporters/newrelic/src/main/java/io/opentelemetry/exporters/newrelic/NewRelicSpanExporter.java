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

package io.opentelemetry.exporters.newrelic;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.exceptions.ResponseException;
import com.newrelic.telemetry.exceptions.RetryWithBackoffException;
import com.newrelic.telemetry.exceptions.RetryWithRequestedWaitException;
import com.newrelic.telemetry.spans.SpanBatch;
import com.newrelic.telemetry.spans.SpanBatchSender;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.List;

/**
 * The NewRelicSpanExporter takes a list of Span objects, converts them into a New Relic SpanBatch
 * instance and then sends it to the New Relic trace ingest API via a SpanBatchSender.
 *
 * @since 0.1.0
 */
public class NewRelicSpanExporter implements SpanExporter {

  private final SpanBatchAdapter adapter;
  private final SpanBatchSender spanBatchSender;

  /**
   * Constructor for the NewRelicSpanExporter.
   *
   * @param adapter An instance of SpanBatchAdapter that can turn list of open telemetry spans into
   *     New Relic SpanBatch.
   * @param spanBatchSender An instance that sends a SpanBatch to the New Relic trace ingest API
   * @since 0.1.0
   */
  NewRelicSpanExporter(SpanBatchAdapter adapter, SpanBatchSender spanBatchSender) {
    if (spanBatchSender == null) {
      throw new IllegalArgumentException("You must provide a non-null SpanBatchSender");
    }
    this.adapter = adapter;
    this.spanBatchSender = spanBatchSender;
  }

  /**
   * export() is the primary interface action method of all SpanExporters.
   *
   * @param openTracingSpans A list of spans to export to New Relic trace ingest API
   * @return A ResultCode that indicates the execution status of the export operation
   */
  @Override
  public ResultCode export(List<Span> openTracingSpans) {
    try {
      SpanBatch spanBatch = adapter.adaptToSpanBatch(openTracingSpans);
      spanBatchSender.sendBatch(spanBatch);
      return ResultCode.SUCCESS;
    } catch (RetryWithRequestedWaitException | RetryWithBackoffException e) {
      return ResultCode.FAILED_RETRYABLE;
    } catch (ResponseException e) {
      return ResultCode.FAILED_NOT_RETRYABLE;
    }
  }

  @Override
  public void shutdown() {}

  /**
   * Creates a new builder instance.
   *
   * @return a new instance builder for this exporter.
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Builder utility for this exporter. Note that the SpanBatchSender must be provided.
   *
   * @since 0.1.0
   */
  public static class Builder {

    private Attributes commonAttributes = new Attributes();
    private SpanBatchSender spanBatchSender;

    /**
     * A SpanBatchSender from the New Relic Telemetry SDK. This is a required field.
     *
     * @param spanBatchSender the sender to use.
     * @return this builder's instance
     */
    public Builder setSpanBatchSender(SpanBatchSender spanBatchSender) {
      this.spanBatchSender = spanBatchSender;
      return this;
    }

    /**
     * A set of attributes that should be attached to all Spans that are sent to New Relic.
     *
     * @param commonAttributes the attributes to attach
     * @return this builder's instance
     */
    public Builder commonAttributes(Attributes commonAttributes) {
      this.commonAttributes = commonAttributes;
      return this;
    }

    /**
     * Constructs a new instance of the exporter based on the builder's values.
     *
     * @return a new NewRelicSpanExporter instance
     */
    public NewRelicSpanExporter build() {
      return new NewRelicSpanExporter(new SpanBatchAdapter(commonAttributes), spanBatchSender);
    }
  }
}
