package com.example.loanmcp.domain;

/**
 * Tracks the lifecycle stage of a loan decision.
 *
 * PENDING     — Application received, evaluation not yet started
 * APPROVED    — AI and Drools both approved
 * REJECTED    — Rejected by validation, fraud check, or Drools rules
 * OVERRIDDEN  — AI approved but Drools rules forced a rejection
 */
public enum WorkflowStatus {
    PENDING,
    APPROVED,
    REJECTED,
    OVERRIDDEN
}
