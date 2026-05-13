package com.example.loanmcp.audit;

import com.example.loanmcp.domain.LoanApplication;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Persistent audit record for every tool invocation.
 * Indexed on loan_application_id for efficient per-application queries: SELECT * FROM tool_trace WHERE loan_application_id = ?
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tool_trace", indexes = @Index(name = "idx_tool_trace_app_id", columnList = "loan_application_id"))
public class ToolTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the tool invoked: validate, creditScore, fraudCheck, evaluateRules.
     * Indexed implicitly via the composite query pattern; add a separate index
     * if you query by toolName alone in production.
     */
    @Column(nullable = false)
    private String toolName;

    /**
     * Full serialized input passed to the tool.
     * TEXT type — loan application data can exceed VARCHAR(255).
     */
    @Column(columnDefinition = "TEXT")
    private String input;

    /**
     * Serialized output returned by the tool.
     * TEXT type for same reason as input.
     */
    @Column(columnDefinition = "TEXT")
    private String output;

    /**
     * Wall-clock execution time in milliseconds — used for performance monitoring.
     */
    private long durationMs;

    /**
     * UTC timestamp of invocation — always store in UTC, display in local time.
     */
    private Instant timestamp;

    /**
     * FK to the loan application this trace belongs to.
     * FetchType.LAZY — we rarely need the full application when querying traces.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id")
    private LoanApplication loanApplication;

    /**
     * Convenience constructor — keeps call sites clean.
     */
    public ToolTrace(String toolName, String input, String output,
                     long durationMs, LoanApplication loanApplication) {
        this.toolName = toolName;
        this.input = input;
        this.output = output;
        this.durationMs = durationMs;
        this.loanApplication = loanApplication;
        this.timestamp = Instant.now();
    }
}