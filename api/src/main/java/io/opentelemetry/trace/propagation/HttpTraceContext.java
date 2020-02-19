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

package io.opentelemetry.trace.propagation;

import static io.opentelemetry.internal.Utils.checkArgument;
import static io.opentelemetry.internal.Utils.checkNotNull;

import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.concurrent.Immutable;

/**
 * Implementation of the TraceContext propagation protocol. See <a
 * href=https://github.com/w3c/distributed-tracing>w3c/distributed-tracing</a>.
 */
@Immutable
public class HttpTraceContext implements HttpTextFormat<SpanContext> {

  private static final TraceState TRACE_STATE_DEFAULT = TraceState.builder().build();
  static final String TRACE_PARENT = "traceparent";
  static final String TRACE_STATE = "tracestate";
  private static final List<String> FIELDS =
      Collections.unmodifiableList(Arrays.asList(TRACE_PARENT, TRACE_STATE));

  private static final String VERSION = "00";
  private static final int VERSION_SIZE = 2;
  private static final char TRACEPARENT_DELIMITER = '-';
  private static final int TRACEPARENT_DELIMITER_SIZE = 1;
  private static final int TRACE_ID_HEX_SIZE = 2 * TraceId.getSize();
  private static final int SPAN_ID_HEX_SIZE = 2 * SpanId.getSize();
  private static final int TRACE_OPTION_HEX_SIZE = 2 * TraceFlags.getSize();
  private static final int TRACE_ID_OFFSET = VERSION_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int SPAN_ID_OFFSET =
      TRACE_ID_OFFSET + TRACE_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int TRACE_OPTION_OFFSET =
      SPAN_ID_OFFSET + SPAN_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int TRACEPARENT_HEADER_SIZE = TRACE_OPTION_OFFSET + TRACE_OPTION_HEX_SIZE;
  private static final int TRACESTATE_MAX_SIZE = 512;
  private static final String TRACESTATE_ENTRY_DELIMITER = ",";
  private static final Pattern ENTRY_PATTERN = Pattern.compile(",");
  static final SpanContext INVALID_SPAN_CONTEXT =
      SpanContext.create(
          TraceId.getInvalid(),
          SpanId.getInvalid(),
          TraceFlags.getDefault(),
          TraceState.getDefault());

  @Override
  public List<String> fields() {
    return FIELDS;
  }

  @Override
  public <C> void inject(SpanContext spanContext, C carrier, Setter<C> setter) {
    checkNotNull(spanContext, "spanContext");
    checkNotNull(setter, "setter");
    checkNotNull(carrier, "carrier");
    char[] chars = new char[TRACEPARENT_HEADER_SIZE];
    chars[0] = VERSION.charAt(0);
    chars[1] = VERSION.charAt(1);
    chars[2] = TRACEPARENT_DELIMITER;
    spanContext.getTraceId().copyLowerBase16To(chars, TRACE_ID_OFFSET);
    chars[SPAN_ID_OFFSET - 1] = TRACEPARENT_DELIMITER;
    spanContext.getSpanId().copyLowerBase16To(chars, SPAN_ID_OFFSET);
    chars[TRACE_OPTION_OFFSET - 1] = TRACEPARENT_DELIMITER;
    spanContext.getTraceFlags().copyLowerBase16To(chars, TRACE_OPTION_OFFSET);
    setter.set(carrier, TRACE_PARENT, new String(chars));
    List<String> entries = spanContext.getTraceState().getEntries();
    if (entries.isEmpty()) {
      // No need to add an empty "tracestate" header.
      return;
    }
    StringBuilder stringBuilder = new StringBuilder(TRACESTATE_MAX_SIZE);
    for (String entry : entries) {
      if (stringBuilder.length() != 0) {
        stringBuilder.append(TRACESTATE_ENTRY_DELIMITER);
      }
      stringBuilder.append(entry);
    }
    setter.set(carrier, TRACE_STATE, stringBuilder.toString());
  }

  @Override
  public <C /*>>> extends @NonNull Object*/> SpanContext extract(C carrier, Getter<C> getter) {
    checkNotNull(carrier, "carrier");
    checkNotNull(getter, "getter");
    String traceParent = getter.get(carrier, TRACE_PARENT);
    if (traceParent == null) {
      return INVALID_SPAN_CONTEXT;
    }

    SpanContext parsedTraceParent = parseTraceParent(traceParent);

    String traceState = getter.get(carrier, TRACE_STATE);
    if (traceState == null || traceState.isEmpty()) {
      return parsedTraceParent;
    }
    TraceState.Builder traceStateBuilder = TraceState.builder();
    String[] listMembers = ENTRY_PATTERN.split(traceState);

    // Iterate in reverse order because when call builder set the elements is added in the
    // front of the list.
    for (int i = listMembers.length - 1; i >= 0; i--) {
      String listMember = listMembers[i];
      traceStateBuilder.add(listMember);
    }
    return SpanContext.createFromRemoteParent(
        parsedTraceParent.getTraceId(), parsedTraceParent.getSpanId(),
        parsedTraceParent.getTraceFlags(), traceStateBuilder.build());
  }

  private static SpanContext parseTraceParent(String traceparent) {
    try {
      // TODO(bdrutu): Do we need to verify that version is hex and that for the version
      // the length is the expected one?
      checkArgument(
          traceparent.charAt(TRACE_OPTION_OFFSET - 1) == TRACEPARENT_DELIMITER
              && (traceparent.length() == TRACEPARENT_HEADER_SIZE
                  || (traceparent.length() > TRACEPARENT_HEADER_SIZE
                      && traceparent.charAt(TRACEPARENT_HEADER_SIZE) == TRACEPARENT_DELIMITER))
              && traceparent.charAt(SPAN_ID_OFFSET - 1) == TRACEPARENT_DELIMITER
              && traceparent.charAt(TRACE_OPTION_OFFSET - 1) == TRACEPARENT_DELIMITER,
          "Missing or malformed TRACEPARENT.");

      TraceId traceId = TraceId.fromLowerBase16(traceparent, TRACE_ID_OFFSET);
      SpanId spanId = SpanId.fromLowerBase16(traceparent, SPAN_ID_OFFSET);
      TraceFlags traceFlags = TraceFlags.fromLowerBase16(traceparent, TRACE_OPTION_OFFSET);
      return SpanContext.createFromRemoteParent(traceId, spanId, traceFlags, TRACE_STATE_DEFAULT);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid traceparent: " + traceparent, e);
    }
  }
}
