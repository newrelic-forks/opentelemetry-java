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

package io.opentelemetry.sdk.trace;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.trace.AttributeValue;
import io.opentelemetry.trace.Link;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import java.util.List;
import java.util.Map;

/**
 * The extend Span interface used by the SDK.
 */
public interface ReadableSpan {

  /**
   * Returns the {@link SpanContext} of the {@code Span}.
   *
   * <p>Equivalent with {@link Span#getContext()}.
   *
   * @return the {@link SpanContext} of the {@code Span}.
   */
  SpanContext getSpanContext();

  /**
   * Returns the name of the {@code Span}.
   *
   * <p>The name can be changed during the lifetime of the Span by using the {@link
   * Span#updateName(String)} so this value cannot be cached.
   *
   * @return the name of the {@code Span}.
   */
  String getName();

  /**
   * Returns the proto representation of the collected data for this particular {@code Span}.
   *
   * @return the proto representation of the collected data for this particular {@code Span}.
   */
  io.opentelemetry.proto.trace.v1.Span toSpanProto();

  long getStartNanoTime();

  long getEndNanoTime();

  Kind getKind();

  SpanId getParentSpanId();

  Resource getResource();

  Status getStatus();

  List<TimedEvent> getEvents();

  List<Link> getLinks();

  Map<String, AttributeValue> getAttributes();
}
