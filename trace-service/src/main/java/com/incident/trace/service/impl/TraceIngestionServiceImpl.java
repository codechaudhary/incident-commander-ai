package com.incident.trace.service.impl;

import com.incident.trace.dto.request.OtlpSpanDto;
import com.incident.trace.dto.request.OtlpTraceRequest;
import com.incident.trace.dto.request.ResourceSpanDto;
import com.incident.trace.dto.request.ScopeSpanDto;
import com.incident.trace.dto.response.TraceIngestResponse;
import com.incident.trace.entity.SpanEntity;
import com.incident.trace.entity.TraceEntity;
import com.incident.trace.event.TraceEventPublisher;
import com.incident.trace.exception.DuplicateTraceException;
import com.incident.trace.exception.InvalidTracePayloadException;
import com.incident.trace.mapper.TraceMapper;
import com.incident.trace.repository.SpanRepository;
import com.incident.trace.repository.TraceRepository;
import com.incident.trace.service.TraceIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TraceIngestionServiceImpl
        implements TraceIngestionService {

    private static final Integer OTEL_ERROR_CODE = 2;

    private final TraceRepository traceRepository;

    private final SpanRepository spanRepository;

    private final TraceMapper traceMapper;

    private final TraceEventPublisher traceEventPublisher;

    @Override
    public TraceIngestResponse ingestTrace(
            OtlpTraceRequest request
    ) {

        validateRequest(request);

        ResourceSpanDto resourceSpan =
                request.getResourceSpans().getFirst();

        List<OtlpSpanDto> allSpans =
                collectAllSpans(resourceSpan);

        if (allSpans.isEmpty()) {

            log.warn(
                    "Trace ingestion rejected. Reason=no spans found"
            );

            throw new InvalidTracePayloadException(
                    "No spans found in request"
            );
        }

        OtlpSpanDto rootSpan =
                findRootSpan(allSpans);

        log.info(
                "Starting trace ingestion. traceId={}",
                rootSpan.getTraceId()
        );

        String serviceName =
                traceMapper.extractServiceName(
                        resourceSpan
                );

        TraceEntity traceEntity =
                traceMapper.toTraceEntity(
                        resourceSpan,
                        rootSpan
                );

        applyOverallTraceStatus(
                traceEntity,
                allSpans
        );

        if (traceRepository.findByTraceId(
                traceEntity.getTraceId()
        ).isPresent()) {

            log.warn(
                    "Duplicate trace received. traceId={}",
                    traceEntity.getTraceId()
            );

            throw new DuplicateTraceException(
                    traceEntity.getTraceId()
            );
        }

        traceRepository.save(traceEntity);

        List<SpanEntity> spanEntities =
                buildSpanEntities(
                        allSpans,
                        traceEntity,
                        serviceName
                );

        spanRepository.saveAll(spanEntities);

        log.info(
                "Trace persisted successfully. traceId={} spanCount={} status={}",
                traceEntity.getTraceId(),
                spanEntities.size(),
                traceEntity.getStatus()
        );

        /*
         * Publish Spring Application Event.
         *
         * Kafka publication will happen ONLY AFTER
         * successful database transaction commit via:
         *
         * @TransactionalEventListener(AFTER_COMMIT)
         */
        traceEventPublisher
                .publishTracePersistedEvent(
                        traceEntity,
                        spanEntities.size()
                );

        return TraceIngestResponse.builder()
                .traceId(
                        traceEntity.getTraceId()
                )
                .spanCount(
                        spanEntities.size()
                )
                .status(
                        "INGESTED"
                )
                .build();
    }

    private void validateRequest(
            OtlpTraceRequest request
    ) {

        if (request == null ||
                request.getResourceSpans() == null ||
                request.getResourceSpans().isEmpty()) {

            log.warn(
                    "Trace ingestion rejected. Reason=resourceSpans missing"
            );

            throw new InvalidTracePayloadException(
                    "resourceSpans cannot be empty"
            );
        }
    }

    private List<OtlpSpanDto> collectAllSpans(
            ResourceSpanDto resourceSpan
    ) {

        List<OtlpSpanDto> spans =
                new ArrayList<>();

        if (resourceSpan.getScopeSpans() == null) {
            return spans;
        }

        for (ScopeSpanDto scopeSpan :
                resourceSpan.getScopeSpans()) {

            if (scopeSpan.getSpans() != null) {

                spans.addAll(
                        scopeSpan.getSpans()
                );
            }
        }

        return spans;
    }

    private OtlpSpanDto findRootSpan(
            List<OtlpSpanDto> spans
    ) {

        for (OtlpSpanDto span : spans) {

            if (span.getParentSpanId() == null ||
                    span.getParentSpanId().isBlank()) {

                return span;
            }
        }

        log.warn(
                "Trace ingestion rejected. Reason=root span missing"
        );

        throw new InvalidTracePayloadException(
                "Root span not found"
        );
    }

    private List<SpanEntity> buildSpanEntities(
            List<OtlpSpanDto> spans,
            TraceEntity traceEntity,
            String serviceName
    ) {

        List<SpanEntity> spanEntities =
                new ArrayList<>();

        for (OtlpSpanDto span : spans) {

            spanEntities.add(
                    traceMapper.toSpanEntity(
                            span,
                            traceEntity,
                            serviceName
                    )
            );
        }

        return spanEntities;
    }

    private void applyOverallTraceStatus(
            TraceEntity traceEntity,
            List<OtlpSpanDto> spans
    ) {

        for (OtlpSpanDto span : spans) {

            if (span.getStatus() != null &&
                    OTEL_ERROR_CODE.equals(
                            span.getStatus().getCode()
                    )) {

                traceEntity.setStatus(
                        com.incident.trace.enums.TraceStatus.ERROR
                );

                traceEntity.setFailureType(
                        traceMapper.extractFailureType(
                                span
                        )
                );

                log.info(
                        "Error span detected. traceId={} failureType={}",
                        traceEntity.getTraceId(),
                        traceEntity.getFailureType()
                );

                return;
            }
        }
    }
}