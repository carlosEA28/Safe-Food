package com.carlos.inspection_service.exceptions;

public class ChecklistConflictException extends RuntimeException {
    public ChecklistConflictException(String message) { super(message); }
}
