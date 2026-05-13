package com.example.loanmcp.repository;

import com.example.loanmcp.audit.ToolTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolTraceRepository extends JpaRepository<ToolTrace, Long> {

    /**
     * Fetch all tool traces for a given loan application.
     * Used by audit endpoints and debugging workflows.
     * The idx_tool_trace_app_id index makes this query efficient at scale.
     */
    List<ToolTrace> findByLoanApplicationId(Long loanApplicationId);

    /**
     * Fetch traces by tool name — useful for monitoring specific tool performance
     * or identifying patterns in tool failures across all applications.
     */
    List<ToolTrace> findByToolName(String toolName);
}