package io.opentelemetry.sdk.trace;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.sdk.trace.export.SpanData;
import io.opentelemetry.sdk.trace.export.SpanData.Event;
import io.opentelemetry.sdk.trace.export.SpanData.TimedEvent;
import io.opentelemetry.sdk.trace.export.SpanData.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** An adapter that can convert a ReadableSpan into a SpanData. */
public class ReadableSpanAdapter {

  private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

  /**
   * Converts a ReadableSpan into a new instance of SpanData
   *
   * @param span A ReadableSpan
   * @return A newly created SpanData instance based on the data in the ReadableSpan
   */
  public SpanData adapt(ReadableSpan span) {
    return SpanData.newBuilder()
        .name(span.getName())
        .context(span.getSpanContext())
        .attributes(span.getAttributes())
        .startTimestamp(nanoToTimestamp(span.getStartNanoTime()))
        .endTimestamp(nanoToTimestamp(span.getEndNanoTime()))
        .kind(span.getKind())
        .links(span.getLinks())
        .parentSpanId(span.getParentSpanId())
        .resource(span.getResource())
        .status(span.getStatus())
        .timedEvents(adaptTimedEvents(span))
        .build();
  }

  private List<TimedEvent> adaptTimedEvents(ReadableSpan span) {
    List<io.opentelemetry.sdk.trace.TimedEvent> sourceEvents = span.getEvents();
    List<TimedEvent> result = new ArrayList<>(sourceEvents.size());
    for (io.opentelemetry.sdk.trace.TimedEvent sourceEvent : sourceEvents) {
      result.add(adaptTimedEvent(sourceEvent));
    }
    return result;
  }

  private TimedEvent adaptTimedEvent(io.opentelemetry.sdk.trace.TimedEvent sourceEvent) {
    Timestamp timestamp = nanoToTimestamp(sourceEvent.getNanotime());
    io.opentelemetry.trace.Event event =
        Event.create(sourceEvent.getName(), sourceEvent.getAttributes());
    return TimedEvent.create(timestamp, event);
  }

  @VisibleForTesting
  static Timestamp nanoToTimestamp(long nanotime) {
    return Timestamp.create(nanotime / NANOS_PER_SECOND, (int) (nanotime % NANOS_PER_SECOND));
  }
}
