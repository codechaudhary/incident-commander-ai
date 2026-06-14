package com.incident.trace.mapper;

import com.incident.trace.dto.request.AttributeDto;
import com.incident.trace.dto.request.AttributeValueDto;
import com.incident.trace.dto.request.OtlpEventDto;
import com.incident.trace.dto.request.OtlpSpanDto;
import com.incident.trace.dto.request.OtlpStatusDto;
import com.incident.trace.dto.request.ResourceSpanDto;
import com.incident.trace.entity.SpanEntity;
import com.incident.trace.entity.TraceEntity;
import com.incident.trace.enums.FailureType;
import com.incident.trace.enums.SpanStatus;
import com.incident.trace.enums.TraceStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TraceMapper {

    private static final Integer OTEL_ERROR_CODE = 2;

    private static final String SERVICE_NAME_KEY =
            "service.name";

    private static final String FAILURE_TYPE_KEY =
            "failure.type";

    private static final String UNKNOWN_SERVICE =
            "unknown-service";

    public TraceEntity toTraceEntity(ResourceSpanDto resourceSpanDto, OtlpSpanDto rootSpan) {

        return TraceEntity.builder()
                .traceId(rootSpan.getTraceId())
                .rootService(extractServiceName(resourceSpanDto))
                .rootOperation(rootSpan.getName())
                .status(mapTraceStatus(rootSpan.getStatus()))
                .failureType(extractFailureType(rootSpan))
                .durationMs(calculateDuration(rootSpan))
                .startedAt(toInstant(rootSpan.getStartTimeUnixNano()))
                .endedAt(toInstant(rootSpan.getEndTimeUnixNano()))
                .build();
    }

    public SpanEntity toSpanEntity(
            OtlpSpanDto spanDto,
            TraceEntity traceEntity,
            String serviceName
    ) {

        return SpanEntity.builder()
                .trace(traceEntity)
                .spanId(spanDto.getSpanId())
                .parentSpanId(spanDto.getParentSpanId() == null || spanDto.getParentSpanId().isBlank() ? null : spanDto.getParentSpanId()
                )
                .serviceName(serviceName)
                .operation(spanDto.getName())
                .status(
                        mapSpanStatus(spanDto.getStatus())
                )
                .durationMs(
                        calculateDuration(spanDto)
                )
                .startedAt(
                        toInstant(spanDto.getStartTimeUnixNano())
                )
                .endedAt(
                        toInstant(spanDto.getEndTimeUnixNano())
                )
                .attributes(
                        extractAttributes(spanDto)
                )
                .events(
                        extractEvents(spanDto)
                )
                .build();
    }

    public String extractServiceName(ResourceSpanDto resourceSpanDto) {

        if (resourceSpanDto == null ||
                resourceSpanDto.getResource() == null ||
                resourceSpanDto.getResource().getAttributes() == null) {

            return UNKNOWN_SERVICE;
        }

        for (AttributeDto attribute :
                resourceSpanDto.getResource().getAttributes()) {

            if (SERVICE_NAME_KEY.equals(attribute.getKey())
                    && attribute.getValue() != null
                    && attribute.getValue().getStringValue() != null) {

                return attribute.getValue().getStringValue();
            }
        }

        return UNKNOWN_SERVICE;
    }

    public FailureType extractFailureType(
            OtlpSpanDto spanDto
    ) {

        if (spanDto.getAttributes() == null) {
            return FailureType.NONE;
        }

        for (AttributeDto attribute :
                spanDto.getAttributes()) {

            if (!FAILURE_TYPE_KEY.equals(attribute.getKey())) {
                continue;
            }

            if (attribute.getValue() == null ||
                    attribute.getValue().getStringValue() == null) {

                return FailureType.NONE;
            }

            try {

                return FailureType.valueOf(
                        attribute.getValue().getStringValue()
                );

            } catch (Exception exception) {

                return FailureType.NONE;
            }
        }

        return FailureType.NONE;
    }

    private TraceStatus mapTraceStatus(
            OtlpStatusDto status
    ) {

        return isError(status)
                ? TraceStatus.ERROR
                : TraceStatus.SUCCESS;
    }

    private SpanStatus mapSpanStatus(
            OtlpStatusDto status
    ) {

        return isError(status)
                ? SpanStatus.ERROR
                : SpanStatus.OK;
    }

    private boolean isError(
            OtlpStatusDto status
    ) {

        return status != null
                && OTEL_ERROR_CODE.equals(
                status.getCode()
        );
    }

    private Long calculateDuration(
            OtlpSpanDto spanDto
    ) {

        long startNano =
                Long.parseLong(
                        spanDto.getStartTimeUnixNano()
                );

        long endNano =
                Long.parseLong(
                        spanDto.getEndTimeUnixNano()
                );

        long durationNanos =
                endNano - startNano;

        return Math.max(
                0,
                durationNanos / 1_000_000
        );
    }

    private Instant toInstant(
            String unixNano
    ) {

        long nano =
                Long.parseLong(
                        unixNano
                );

        long seconds =
                nano / 1_000_000_000;

        long nanos =
                nano % 1_000_000_000;

        return Instant.ofEpochSecond(
                seconds,
                nanos
        );
    }

    private Map<String, Object> extractAttributes(
            OtlpSpanDto spanDto
    ) {

        Map<String, Object> attributes =
                new HashMap<>();

        if (spanDto.getAttributes() == null) {
            return attributes;
        }

        for (AttributeDto attribute :
                spanDto.getAttributes()) {

            attributes.put(
                    attribute.getKey(),
                    extractAttributeValue(attribute)
            );
        }

        return attributes;
    }

    private Object extractAttributeValue(
            AttributeDto attribute
    ) {

        if (attribute == null ||
                attribute.getValue() == null) {

            return null;
        }

        AttributeValueDto value =
                attribute.getValue();

        if (value.getStringValue() != null) {
            return value.getStringValue();
        }

        if (value.getIntValue() != null) {
            return value.getIntValue();
        }

        if (value.getDoubleValue() != null) {
            return value.getDoubleValue();
        }

        if (value.getBoolValue() != null) {
            return value.getBoolValue();
        }

        return null;
    }

    private List<Map<String, Object>> extractEvents(
            OtlpSpanDto spanDto
    ) {

        List<Map<String, Object>> events =
                new ArrayList<>();

        if (spanDto.getEvents() == null) {
            return events;
        }

        for (OtlpEventDto eventDto :
                spanDto.getEvents()) {

            Map<String, Object> event =
                    new HashMap<>();

            event.put(
                    "name",
                    eventDto.getName()
            );

            event.put(
                    "timeUnixNano",
                    eventDto.getTimeUnixNano()
            );

            if (eventDto.getAttributes() != null) {

                Map<String, Object> eventAttributes =
                        new HashMap<>();

                for (AttributeDto attribute :
                        eventDto.getAttributes()) {

                    eventAttributes.put(
                            attribute.getKey(),
                            extractAttributeValue(attribute)
                    );
                }

                event.put(
                        "attributes",
                        eventAttributes
                );
            }

            events.add(event);
        }

        return events;
    }
}