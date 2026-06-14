package com.incident.trace.service.impl;

import com.incident.trace.dto.response.PagedResponse;
import com.incident.trace.dto.response.SpanResponse;
import com.incident.trace.dto.response.TraceDetailResponse;
import com.incident.trace.dto.response.TraceSummaryResponse;
import com.incident.trace.entity.SpanEntity;
import com.incident.trace.entity.TraceEntity;
import com.incident.trace.enums.TraceStatus;
import com.incident.trace.exception.TraceNotFoundException;
import com.incident.trace.repository.SpanRepository;
import com.incident.trace.repository.TraceRepository;
import com.incident.trace.service.TraceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TraceQueryServiceImpl
        implements TraceQueryService {

    private final TraceRepository traceRepository;

    private final SpanRepository spanRepository;

    @Override
    public TraceDetailResponse getTraceByTraceId(
            String traceId
    ) {

        TraceEntity trace =
                traceRepository
                        .findByTraceId(traceId)
                        .orElseThrow(() ->
                                new TraceNotFoundException(
                                        traceId
                                ));

        List<SpanResponse> spans =
                spanRepository
                        .findByTrace_TraceId(traceId)
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        SpanEntity::getStartedAt
                                )
                        )
                        .map(this::mapSpanResponse)
                        .toList();

        return TraceDetailResponse.builder()
                .traceId(trace.getTraceId())
                .rootService(trace.getRootService())
                .rootOperation(trace.getRootOperation())
                .status(trace.getStatus())
                .failureType(trace.getFailureType())
                .durationMs(trace.getDurationMs())
                .startedAt(trace.getStartedAt())
                .endedAt(trace.getEndedAt())
                .createdAt(trace.getCreatedAt())
                .spans(spans)
                .build();
    }

    @Override
    public PagedResponse<TraceSummaryResponse> getTraces(
            TraceStatus status,
            Instant from,
            Instant to,
            int page,
            int size
    ) {

        PageRequest pageRequest =
                PageRequest.of(page, Math.min(size, 100));

        Page<TraceEntity> traces;

        if (status != null &&
                from != null &&
                to != null) {

            traces =
                    traceRepository
                            .findByStatusAndStartedAtBetween(
                                    status,
                                    from,
                                    to,
                                    pageRequest
                            );

        } else if (status != null) {

            traces =
                    traceRepository
                            .findByStatus(
                                    status,
                                    pageRequest
                            );

        } else if (from != null &&
                to != null) {

            traces =
                    traceRepository
                            .findByStartedAtBetween(
                                    from,
                                    to,
                                    pageRequest
                            );

        } else {

            traces =
                    traceRepository.findAll(
                            pageRequest
                    );
        }

        List<TraceSummaryResponse> items =
                traces.getContent()
                        .stream()
                        .map(this::mapSummaryResponse)
                        .toList();

        return PagedResponse.<TraceSummaryResponse>builder()
                .content(items)
                .page(traces.getNumber())
                .size(traces.getSize())
                .totalElements(
                        traces.getTotalElements()
                )
                .totalPages(
                        traces.getTotalPages()
                )
                .last(
                        traces.isLast()
                )
                .build();
    }

    private TraceSummaryResponse mapSummaryResponse(
            TraceEntity trace
    ) {

        return TraceSummaryResponse.builder()
                .traceId(trace.getTraceId())
                .rootService(trace.getRootService())
                .rootOperation(trace.getRootOperation())
                .status(trace.getStatus())
                .failureType(trace.getFailureType())
                .durationMs(trace.getDurationMs())
                .startedAt(trace.getStartedAt())
                .endedAt(trace.getEndedAt())
                .createdAt(trace.getCreatedAt())
                .spanCount(Math.toIntExact(spanRepository.countByTrace_TraceId(trace.getTraceId())))
                .build();
    }

    private SpanResponse mapSpanResponse(
            SpanEntity span
    ) {

        return SpanResponse.builder()
                .spanId(span.getSpanId())
                .parentSpanId(
                        span.getParentSpanId()
                )
                .serviceName(
                        span.getServiceName()
                )
                .operation(
                        span.getOperation()
                )
                .status(
                        span.getStatus()
                )
                .durationMs(
                        span.getDurationMs()
                )
                .startedAt(
                        span.getStartedAt()
                )
                .endedAt(
                        span.getEndedAt()
                )
                .attributes(
                        span.getAttributes()
                )
                .events(
                        span.getEvents()
                )
                .build();
    }
}