package com.example.loanmcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Captures all intermediate tool results for a single loan evaluation session.
 * Passed into the AI prompt as structured context so the LLM can reason
 * about what has already been done and what remains.
 */
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanWorkflowContext {

    private Long applicationId;
    private String validationResult;
    private Integer creditScore;
    private Boolean fraudDetected;
    private List<String> ruleOutcomes;

    // Derived fields the LLM can use to short-circuit decisions
    public boolean isValidationPassed() {
        return "PASS".equals(validationResult);
    }

    public boolean isFraudSuspected() {
        return Boolean.TRUE.equals(fraudDetected);
    }

    public boolean hasRuleApproval() {
        return ruleOutcomes != null && ruleOutcomes.contains("APPROVED");
    }

    /**
     * Human-readable summary injected into the prompt template.
     * Keep this concise — every token costs inference time and money.
     */
    @Override
    public String toString() {
        return String.format(
                "WorkflowContext{applicationId=%d, validation='%s', creditScore=%s, " +
                        "fraudDetected=%s, ruleOutcomes=%s}",
                applicationId, validationResult, creditScore, fraudDetected, ruleOutcomes
        );
    }
}
