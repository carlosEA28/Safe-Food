package com.carlos.inspection_service.dto;

import java.time.Instant;

public record ErrorResponse(String code, String message, Instant timestamp) {}
