package com.example.loanmcp.tools;

import com.example.loanmcp.domain.LoanApplication;
import com.example.loanmcp.service.RulesEngineService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Contains all shared business logic for loan tool operations.
 * Neither ApiLoanTools nor McpLoanTools duplicates this logic —
 * they both delegate here, differing only in how they handle persistence
 * and tracing before/after the call.
 */
@Component
public class LoanToolsDelegate {

    private final RulesEngineService rulesEngineService;

    public LoanToolsDelegate(RulesEngineService rulesEngineService) {
        this.rulesEngineService = rulesEngineService;
    }

    public ToolResult<String> doAgeValidation(LoanApplication app) {
        long start = System.currentTimeMillis();
        try {
            String result = app.getAge() < 21
                    ? "FAIL: Applicant must be at least 21 years old"
                    : "PASS";
            return ToolResult.success("ageValidation", result, elapsed(start));
        } catch (Exception e) {
            return ToolResult.failure("ageValidation", e.getMessage(), elapsed(start));
        }
    }

    public ToolResult<Integer> doCreditScore(LoanApplication app) {
        long start = System.currentTimeMillis();
        int score = 0;
        try {
            // TODO: Replace with real bureau integration
            if(app.getCreditScore() != 0 && app.getCreditScore() > 0){
                score = app.getCreditScore();
            }else{
                score = new Random().nextInt(300) + 550;
            }
            app.setCreditScore(score);
            return ToolResult.success("creditScore", score, elapsed(start));
        } catch (Exception e) {
            return ToolResult.failure("creditScore", e.getMessage(), elapsed(start));
        }
    }

    public ToolResult<Boolean> doFraudCheck(LoanApplication app) {
        long start = System.currentTimeMillis();
        try {
            boolean fraud = app.getIncome() < 1000
                    && app.getExistingLoanAmount() > 50_000;
            return ToolResult.success("fraudCheck", fraud, elapsed(start));
        } catch (Exception e) {
            return ToolResult.failure("fraudCheck", e.getMessage(), elapsed(start));
        }
    }

    public ToolResult<List<String>> doEvaluateRules(LoanApplication app) {
        long start = System.currentTimeMillis();
        try {
            List<String> results = rulesEngineService.evaluate(app);
            return ToolResult.success("evaluateDroolRules", results, elapsed(start));
        } catch (Exception e) {
            return ToolResult.failure("evaluateDroolRules", e.getMessage(), elapsed(start));
        }
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}