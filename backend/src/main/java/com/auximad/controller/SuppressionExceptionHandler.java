package com.auximad.controller;

import com.auximad.service.SuppressionConfirmationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class SuppressionExceptionHandler {
    @ExceptionHandler(SuppressionConfirmationException.class)
    public ResponseEntity<?> confirmation(SuppressionConfirmationException e) {
        return ResponseEntity.status(409).body(Map.of("code", "CONFIRMATION_SUPPRESSION", "impact", e.getImpact()));
    }
    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<?> conflit(RuntimeException e) {
        return ResponseEntity.status(409).body(Map.of("message",
                "Les données liées ont changé ou une contrainte empêche cette opération. Aucune modification partielle n'a été conservée."));
    }
}