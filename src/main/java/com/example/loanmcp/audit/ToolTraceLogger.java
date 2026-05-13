package com.example.loanmcp.audit;

import com.example.loanmcp.domain.LoanApplication;
import com.example.loanmcp.repository.LoanApplicationRepository;
import com.example.loanmcp.repository.ToolTraceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * All traces are now persisted into the tool_trace table.
 */
@Component
public class ToolTraceLogger {

    private final ToolTraceRepository traceRepository;
    private final LoanApplicationRepository applicationRepo;

    public ToolTraceLogger(ToolTraceRepository traceRepository, LoanApplicationRepository applicationRepo) {
        this.traceRepository = traceRepository;
        this.applicationRepo = applicationRepo;
    }

    /**
     * Persists a tool trace linked to its loan application.
     * Call this from each @Tool method after execution completes.
     *
     * @param toolName    name of the tool that was invoked
     * @param input       serialized tool input
     * @param output      serialized tool output
     * @param durationMs  execution time in milliseconds
     * @param app         the loan application this trace belongs to
     */
    @Transactional
    public void log(String toolName, String input, String output,
                    long durationMs, LoanApplication app) {

        // If the app hasn't been saved yet (MCP direct tool call path),
        // persist it first so the FK reference is valid.
        if (app.getId() == null) {
            app = applicationRepo.save(app);
        }

        ToolTrace trace = new ToolTrace(toolName, input, output, durationMs, app);
        traceRepository.save(trace);

        // Keep console logging for local development visibility
        System.out.printf("[TRACE] tool=%s | app=%s | duration=%dms | result=%s%n",
                toolName, app.getId(), durationMs, output);

    }

    /**
     * Returns all traces for a specific loan application.
     * Exposed via audit endpoint — replaces the previous getAll()
     * which returned traces for ALL applications indiscriminately.
     */
    public List<ToolTrace> getByApplication(Long applicationId) {
        return traceRepository.findByLoanApplicationId(applicationId);
    }
}