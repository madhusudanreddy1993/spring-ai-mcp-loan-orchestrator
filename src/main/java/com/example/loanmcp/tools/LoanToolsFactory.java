
package com.example.loanmcp.tools;

import org.springframework.stereotype.Component;

/**
 * Runtime context router for LoanToolsPort implementations.
 *
 * Currently not called directly — routing is static:
 *   - LoanAgentService hardwires ApiLoanTools into the ChatClient
 *   - MCP server autowires McpLoanTools via @Bean
 *
 * Use this factory if you later need DYNAMIC routing, for example:
 *   - A controller that can run tools in either context based on a request param
 *   - A batch service that selects context based on a job configuration
 *   - An A/B test comparing API vs Inspector tool behaviour
 */
@Component
public class LoanToolsFactory {

    public enum Context { API, INSPECTOR }

    private final ApiLoanTools apiTools;
    private final McpLoanTools inspectorTools;

    public LoanToolsFactory(ApiLoanTools apiTools,
                            McpLoanTools inspectorTools) {
        this.apiTools = apiTools;
        this.inspectorTools = inspectorTools;
    }

    public LoanToolsPort getTools(Context context) {
        return switch (context) {
            case API       -> apiTools;
            case INSPECTOR -> inspectorTools;
        };
    }
}