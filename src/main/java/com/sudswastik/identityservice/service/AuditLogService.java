package com.sudswastik.identityservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudswastik.identityservice.domain.AuditLog;
import com.sudswastik.identityservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(String entityType, String entityId, String action, String actorSub,
                    Object oldValue, Object newValue) {
        AuditLog entry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actorSub(actorSub)
                .oldValue(toJson(oldValue))
                .newValue(toJson(newValue))
                .build();
        auditLogRepo.save(entry);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize audit log value", e);
        }
    }
}
