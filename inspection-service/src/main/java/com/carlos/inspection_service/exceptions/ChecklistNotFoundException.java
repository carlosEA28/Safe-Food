package com.carlos.inspection_service.exceptions;

public class ChecklistNotFoundException extends RuntimeException {
    public ChecklistNotFoundException() { super("Checklist not found"); }
}
