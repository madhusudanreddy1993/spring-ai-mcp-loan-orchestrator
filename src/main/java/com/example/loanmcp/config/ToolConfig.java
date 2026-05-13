package com.example.loanmcp.config;

import com.example.loanmcp.tools.McpLoanTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {


    /**
     * Registers McpLoanTools with the MCP server.
     * These are the tools exposed via the /mcp endpoint to the MCP Inspector.
     */
    @Bean("inspectorToolCallbackProvider")
    public ToolCallbackProvider inspectorToolCallbackProvider(McpLoanTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}