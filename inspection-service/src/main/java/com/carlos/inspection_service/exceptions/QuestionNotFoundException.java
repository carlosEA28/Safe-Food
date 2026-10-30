package com.carlos.inspection_service.exceptions;

public class QuestionNotFoundException extends RuntimeException {
    public QuestionNotFoundException() { super("Question not found in checklist"); }
}
