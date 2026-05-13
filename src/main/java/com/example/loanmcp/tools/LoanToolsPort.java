
package com.example.loanmcp.tools;

import com.example.loanmcp.domain.LoanApplication;
import java.util.List;

/**
 * Defines the contract for all loan tool implementations.
 * Both API-context and Inspector-context tools implement this interface,
 * allowing the orchestration layer to remain context-agnostic.
 */
public interface LoanToolsPort {

    ToolResult<String>       validateAge(LoanApplication app);
    ToolResult<Integer>      fetchCreditScore(LoanApplication app);
    ToolResult<Boolean>      performFraudCheck(LoanApplication app);
    ToolResult<List<String>> evaluateLoanRules(LoanApplication app);
}