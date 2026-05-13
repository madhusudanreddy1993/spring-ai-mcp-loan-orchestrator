package com.example.loanmcp.tools;

import com.example.loanmcp.audit.ToolTraceLogger;
import com.example.loanmcp.domain.LoanApplication;
import com.example.loanmcp.repository.LoanApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inspector-context tool implementation.
 * <p>
 * Used when the MCP Inspector invokes tools directly,
 * bypassing LoanAgentService and LLM orchestration entirely.
 * <p>
 * Key responsibilities:
 * <p>
 * 1. Persist LoanApplication if not already saved
 * 2. Execute tools independently for manual inspection/testing
 * 3. Provide inspector-friendly responses with rich metadata
 * 4. Persist tool tracing for observability and debugging
 * <p>
 * Unlike ApiLoanTools:
 * - No AI orchestration occurs here
 * - The inspector user controls execution order manually
 * - Tool responses are optimized for human readability
 */
@Component
public class McpLoanTools implements LoanToolsPort {

    private static final Logger logger = LoggerFactory.getLogger(McpLoanTools.class);

    private final LoanToolsDelegate delegate;
    private final ToolTraceLogger traceLogger;
    private final LoanApplicationRepository applicationRepo;

    public McpLoanTools(LoanToolsDelegate delegate,
                        ToolTraceLogger traceLogger,
                        LoanApplicationRepository applicationRepo) {

        this.delegate = delegate;
        this.traceLogger = traceLogger;
        this.applicationRepo = applicationRepo;
    }

    /**
     * Ensures LoanApplication is persisted before creating
     * ToolTrace foreign-key references.
     * <p>
     * Safe to call multiple times.
     */
    @Transactional
    protected LoanApplication ensurePersisted(
            LoanApplication app) {

        if (app.getId() == null || app.getId() == 0L) {

            app.setId(null);

            return applicationRepo.save(app);
        }

        return app;
    }

    @Tool(description = """
            [INSPECTOR TOOL]

            STEP 1 OF 4.

            Manually validate applicant age and basic eligibility.

            This tool is intended for:
            - workflow inspection
            - debugging
            - manual testing
            - observability analysis

            Returns:
            - PASS
            - FAIL reason

            Recommended first tool during inspection workflows.
            """)
    @Transactional
    public ToolResult<String> validateAge(LoanApplication app) {

        logger.info("validateAge[inspector] executed...");

        app = ensurePersisted(app);

        ToolResult<String> result = delegate.doAgeValidation(app);

        traceLogger.log(
                "validateAge[inspector]",
                app.toString(),
                String.valueOf(result.getData()),
                result.getDurationMs(),
                app
        );

        return formatForInspector(result);
    }

    @Tool(description = """
            [INSPECTOR TOOL]

            STEP 2 OF 4.

            Manually fetch applicant credit score.

            This tool is intended for:
            - workflow inspection
            - debugging
            - manual testing
            - observability analysis

            Returns:
            - credit score between 300 and 850

            Recommended execution after validateAge.
            """)
    @Transactional
    public ToolResult<Integer> fetchCreditScore( LoanApplication app) {

        logger.info("fetchCreditScore[inspector] executed...");

        app = ensurePersisted(app);

        ToolResult<Integer> result = delegate.doCreditScore(app);

        traceLogger.log(
                "fetchCreditScore[inspector]",
                app.toString(),
                String.valueOf(result.getData()),
                result.getDurationMs(),
                app
        );

        return formatForInspector(result);
    }

    @Tool(description = """
            [INSPECTOR TOOL]

            STEP 3 OF 4.

            Manually execute fraud detection checks.

            This tool is intended for:
            - workflow inspection
            - debugging
            - manual testing
            - observability analysis

            Returns:
            - true if fraud suspected
            - false otherwise

            Recommended execution after fetchCreditScore.
            """)
    @Transactional
    public ToolResult<Boolean> performFraudCheck( LoanApplication app) {

        logger.info("performFraudCheck[inspector] executed...");

        app = ensurePersisted(app);

        ToolResult<Boolean> result = delegate.doFraudCheck(app);

        traceLogger.log(
                "performFraudCheck[inspector]",
                app.toString(),
                String.valueOf(result.getData()),
                result.getDurationMs(),
                app
        );

        return formatForInspector(result);
    }

    @Tool(description = """
            [INSPECTOR TOOL]

            STEP 4 OF 4.

            Manually evaluate loan eligibility using
            the Drools rules engine.

            This tool is intended for:
            - workflow inspection
            - debugging
            - rule verification
            - observability analysis

            Returns:
            - all triggered rule outcomes
            - approval/rejection indicators

            Recommended execution after performFraudCheck.
            """)
    @Transactional
    public ToolResult<List<String>> evaluateLoanRules(
            LoanApplication app) {

        logger.info("evaluateLoanRules[inspector] executed...");

        app = ensurePersisted(app);

        ToolResult<List<String>> result = delegate.doEvaluateRules(app);

        traceLogger.log(
                "evaluateLoanRules[inspector]",
                app.toString(),
                String.valueOf(result.getData()),
                result.getDurationMs(),
                app
        );

        return formatForInspector(result);
    }

    /**
     * Enhances ToolResult for MCP Inspector readability.
     * <p>
     * Adds:
     * - execution status
     * - execution duration
     * - timestamps
     * - human-readable summaries
     */
    private <T> ToolResult<T> formatForInspector( ToolResult<T> result) {

        String statusLabel = result.isSuccess() ? "[OK]" : "[FAILED]";

        String message = String.format(
                "%s tool=%s | data=%s | duration=%dms | ts=%s",
                statusLabel,
                result.getToolName(),
                result.getData(),
                result.getDurationMs(),
                result.getTimestamp()
        );

        if (result.isSuccess()) {

            return ToolResult.<T>builder()
                    .toolName(result.getToolName())
                    .success(true)
                    .data(result.getData())
                    .durationMs(result.getDurationMs())
                    .timestamp(result.getTimestamp())
                    .errorMessage(message)
                    .build();
        }

        return ToolResult.<T>builder()
                .toolName(result.getToolName())
                .success(false)
                .errorMessage( message + " | error=" + result.getErrorMessage() )
                .durationMs(result.getDurationMs())
                .timestamp(result.getTimestamp())
                .build();
    }
}
