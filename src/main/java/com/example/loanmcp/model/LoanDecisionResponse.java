package com.example.loanmcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Typed representation of the LLM's final decision JSON.
 * Replaces brittle string.contains("\"approved\": true") checks.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanDecisionResponse {

    @JsonProperty("approved")
    private boolean approved;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("ruleOutcomes")
    private List<String> ruleOutcomes;
}
