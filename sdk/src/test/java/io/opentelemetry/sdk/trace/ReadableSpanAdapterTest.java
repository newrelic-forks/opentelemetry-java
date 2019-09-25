package io.opentelemetry.sdk.trace;

import static org.junit.Assert.assertEquals;

import io.opentelemetry.sdk.internal.Clock;
import io.opentelemetry.sdk.internal.TestClock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.export.SpanData;
import io.opentelemetry.trace.AttributeValue;
import io.opentelemetry.trace.Link;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.Tracestate;
import io.opentelemetry.trace.util.Links;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReadableSpanAdapterTest {

  @Test
  public void testAdapt() throws Exception {

    String name = "GreatSpan";
    Kind kind = Kind.SERVER;
    TraceId traceId = TestUtils.generateRandomTraceId();
    SpanId spanId = TestUtils.generateRandomSpanId();
    SpanId parentSpanId = TestUtils.generateRandomSpanId();
    TraceConfig traceConfig = TraceConfig.getDefault();
    SpanProcessor spanProcessor = NoopSpanProcessor.getInstance();
    Clock clock = TestClock.create();
    Map<String, String> labels = new HashMap<>();
    labels.put("foo", "bar");
    Resource resource = Resource.create(labels);
    Map<String, AttributeValue> attributes = TestUtils.generateRandomAttributes();
    SpanContext context = SpanContext
        .create(traceId, spanId, TraceFlags.getDefault(), Tracestate.getDefault());
    Link link1 = Links.create(context, TestUtils.generateRandomAttributes());
    List<Link> links = Collections.singletonList(link1);

    ReadableSpan readableSpan = RecordEventsReadableSpan
        .startSpan(context, name, kind, parentSpanId, traceConfig, spanProcessor,
            null, clock, resource, attributes, links);

    SpanData expected = SpanData.newBuilder()
        .status(Status.OK)
        .build();

    ReadableSpanAdapter testClass = new ReadableSpanAdapter();
    SpanData result = testClass.adapt(readableSpan);
    assertEquals(expected, result);
  }

}