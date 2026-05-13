package com.example.loanmcp.tools;

import com.example.loanmcp.audit.ToolTraceLogger;
import com.example.loanmcp.domain.LoanApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * API-context tool implementation.
 * <p>
 * Used exclusively when the LLM (via ChatClient) orchestrates tool calls
 * during a loan evaluation triggered by POST /loan/apply.
 * <p>
 * Assumptions in this context:
 * - LoanApplication is already persisted
 * - ToolTrace records are persisted and linked to the application FK
 * - The LLM orchestrates tool sequencing using prompt + tool descriptions
 */
@Component
public class ApiLoanTools implements LoanToolsPort {

    private static final Logger logger =
            LoggerFactory.getLogger(ApiLoanTools.class);

    private final LoanToolsDelegate delegate;
    private final ToolTraceLogger traceLogger;

    public ApiLoanTools(LoanToolsDelegate delegate,
                        ToolTraceLogger traceLogger) {
        this.delegate = delegate;
        this.traceLogger = traceLogger;
    }

    @Tool(description = """
            STEP 1 OF 4.

            Mandatory first tool.

            Validate applicant age and basic eligibility.

            This tool MUST ALWAYS execute first.

            If validation fails:
            - stop workflow immediately
            - reject the loan application
            - do NOT continue to fetchCreditScore

            If validation passes:
            - continue to fetchCreditScore

            Do NOT generate final decision yet.
            """)
    public ToolResult<String> validateAge(LoanApplication app) {

        logger.info("validateAge executed...");

        ToolResult<String> result = delegate.doAgeValidation(app);

        traceLogger.log(
                "validateAge",
                app.toString(),
                String.valueOf(result.getData()),
                result.getDurationMs(),
                app
        );

        return result;
    }

    @Tool(description = """
            STEP 2 OF 4.

            Mandatory second tool.

            Fetch applicant credit score.

            This tool MUST ALWAYS execute after validateAge.

            After this tool completes:
            - continue to performFraudCheck
            - then continue to evaluateLoanRules

            Credit score results are authoritative.

            Do NOT generate final decision yet.
            """)
    public ToolResult<Integer> fetchCreditScore(LoanApplication app) {

        logger.info("fetchCreditScore executed...");

        ToolResult<Integer> result = delegate.doCreditScore(app);

        traceLogger.log(
                "fetchCreditScore",
                app.toString(),
                String.valueOf(result.getData()),
                result.getDurationMs(),
                app
        );

        return result;
    }

    @Tool(description = """
            STEP 3 OF 4.

            Mandatory third tool.

            Perform fraud detection checks on the loan application.

            This tool MUST ALWAYS execute after fetchCreditScore.

            If performFraudCheck returns true:
            - stop workflow immediately
            - reject the loan application
            - do NOT continue to evaluateLoanRules

            If performFraudCheck returns false:
            - continue to evaluateLoanRules

            Fraud detection results are authoritative.

            Do NOT generate final decision yet.
            """)
    public ToolResult<Boolean> performFraudCheck(LoanApplication app) {

        logger.info("performFraudCheck executed...");

        ToolResult<Boolean> result = delegate.doFraudCheck(app);

        traceLogger.log(
                "performFraudCheck",
                app.toString(),
                String.valueOf(result.getData()),
                result.getDurationMs(),
                app
        );

        return result;
    }

    @Tool(description = """
            STEP 4 OF 4.

            Mandatory final tool.

            Evaluate the loan application using the Drools rules engine.

            This tool MUST ALWAYS execute after performFraudCheck.

            The rules engine determines final backend eligibility using:
            - credit score
            - income
            - existing loan exposure
            - validation outcomes
            - fraud detection outcomes

            Backend Drools rules are authoritative over AI reasoning.

            After evaluateLoanRules completes:
            - generate final loan approval or rejection decision
            - explain the decision using all tool outcomes
            - return ONLY valid JSON response

            Do NOT skip this tool under any circumstances.
            """)
    public ToolResult<List<String>> evaluateLoanRules(
            LoanApplication app) {

        logger.info("evaluateLoanRules executed...");

        ToolResult<List<String>> result = delegate.doEvaluateRules(app);

        traceLogger.log(
                "evaluateLoanRules",
                app.toString(),
                String.valueOf(result.getData()),
                result.getDurationMs(),
                app
        );

        return result;
    }
}

