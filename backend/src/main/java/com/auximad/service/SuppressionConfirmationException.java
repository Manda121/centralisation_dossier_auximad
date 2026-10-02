package com.auximad.service;

public class SuppressionConfirmationException extends RuntimeException {
    private final SuppressionService.Impact impact;
    public SuppressionConfirmationException(SuppressionService.Impact impact) {
        super("Confirmation requise avant suppression.");
        this.impact = impact;
    }
    public SuppressionService.Impact getImpact() { return impact; }
}