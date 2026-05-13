package com.example.loanmcp.service;

import com.example.loanmcp.config.ToolConfig;
import com.example.loanmcp.domain.LoanApplication;
import com.example.loanmcp.audit.DecisionAudit;
import com.example.loanmcp.audit.DecisionAuditService;
import com.example.loanmcp.domain.LoanDecision;
import com.example.loanmcp.domain.WorkflowStatus;
import com.example.loanmcp.model.LoanDecisionResponse;
import com.example.loanmcp.model.LoanWorkflowContext;
import com.example.loanmcp.repository.LoanApplicationRepository;
import com.example.loanmcp.repository.LoanDecisionRepository;
import com.example.loanmcp.tools.ApiLoanTools;
import com.example.loanmcp.util.JsonParserUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanAgentService {

    /**
     * Injects the raw prompt file from the classpath.
     * Spring Boot serves classpath resources at startup — if the file
     * is missing, the app fails fast with a clear FileNotFoundException.
     */
    @Value("classpath:prompt-templates/loan-evaluation.txt")
    private Resource promptTemplateResource;

    private final ChatClient chatClient;
    private final DecisionAuditService auditService;
    private final LoanApplicationRepository applicationRepo;
    private final LoanDecisionRepository decisionRepo;
    private final RulesEngineService rulesEngineService;
    private final JsonParserUtil jsonParser;

    public LoanAgentService(ChatClient.Builder builder,
                            ApiLoanTools apiTools,
                            DecisionAuditService auditService,
                            LoanApplicationRepository applicationRepo,
                            LoanDecisionRepository decisionRepo,
                            RulesEngineService rulesEngineService,
                            JsonParserUtil jsonParser) {
        // Pass apiTools directly — Spring AI extracts @Tool methods itself.
        this.chatClient = builder.defaultTools(apiTools).build();
        this.auditService = auditService;
        this.applicationRepo = applicationRepo;
        this.decisionRepo = decisionRepo;
        this.rulesEngineService = rulesEngineService;
        this.jsonParser = jsonParser;
    }

    /**
     * PUBLIC entry point — intentionally NOT transactional.
     *
     * The LLM call can take 2–10 seconds. Wrapping it in a transaction
     * would hold a DB connection open for that entire duration, which
     * exhausts the connection pool under any real load.
     *
     * Pattern: do the slow AI work first, then hand off to a
     * @Transactional method that does only fast DB writes.
     */
    public LoanDecisionResponse evaluateLoan(LoanApplication app) {

        // Step 1: Persist application BEFORE the LLM call so it has an ID.
        //         This save is intentionally outside the main transaction —
        //         we want the application row to survive even if AI fails.
        LoanApplication saved = applicationRepo.save(app);

        // Step 2: Build workflow context (no DB access, pure object construction)
        LoanWorkflowContext context = LoanWorkflowContext.builder()
                .applicationId(saved.getId())
                .build();

        // Step 3: Call the LLM — slow, external, NOT inside a transaction
        String rawResponse = chatClient.prompt()
                .user(u -> u.text(buildPrompt(saved, context)))
                .call()
                .content();

        // Step 4: Parse AI response
        LoanDecisionResponse aiDecision = jsonParser
                .parse(rawResponse, LoanDecisionResponse.class)
                .orElse(fallbackDecision(rawResponse));

        // Step 5: Run Drools independently as backend authority
        List<String> ruleOutcomes = rulesEngineService.evaluate(saved);

        // Step 6: Hand off all results to the @Transactional method for DB writes
        return persistDecision(saved, aiDecision, ruleOutcomes, rawResponse);
    }

    /**
     * PRIVATE transactional method — only called after the slow LLM work is done.
     *
     * All DB writes here are atomic:
     * - LoanDecision save
     * - Audit log
     * If either fails, both roll back. The LoanApplication row is intentionally
     * left intact (it's the source of truth for what was submitted).
     *
     * rollbackFor = Exception.class ensures checked exceptions also trigger rollback.
     * By default Spring only rolls back on RuntimeException.
     */
    @Transactional(rollbackFor = Exception.class)
    protected LoanDecisionResponse persistDecision(LoanApplication saved,
                                                   LoanDecisionResponse aiDecision,
                                                   List<String> ruleOutcomes,
                                                   String rawResponse) {

        // Drools override: if AI approved but rules say reject, backend wins
        boolean droolsApproved = ruleOutcomes.contains("APPROVED")
                && ruleOutcomes.stream().noneMatch(r -> r.startsWith("REJECT"));

        WorkflowStatus status;

        if (aiDecision.isApproved() && !droolsApproved) {
            // AI said yes, Drools said no — override
            aiDecision.setApproved(false);
            aiDecision.setReason("AI approved but overridden by rules engine: " + ruleOutcomes);
            status = WorkflowStatus.OVERRIDDEN;

        } else if (aiDecision.isApproved()) {
            status = WorkflowStatus.APPROVED;
        } else {
            status = WorkflowStatus.REJECTED;
        }

        aiDecision.setRuleOutcomes(ruleOutcomes);

        // Persist decision with workflow status
        LoanDecision decision = new LoanDecision();
        decision.setApproved(aiDecision.isApproved());
        decision.setReason(aiDecision.getReason());
        decision.setDecisionTime(LocalDateTime.now());
        decision.setStatus(status);
        decision.setLoanApplication(saved);
        decisionRepo.save(decision);

        // Audit — also inside this transaction so it rolls back with the decision
        auditService.log(new DecisionAudit(
                String.valueOf(saved.getId()),
                aiDecision.isApproved(),
                rawResponse
        ));

        return aiDecision;
    }


    /**
     * Builds the LLM prompt by loading the external template file
     * and substituting the application and context values.
     */
    private String buildPrompt(LoanApplication app, LoanWorkflowContext context) {
        try {
            // Read the .txt file content as a plain string
            String template = promptTemplateResource
                    .getContentAsString(StandardCharsets.UTF_8);

            // Substitute %s placeholders — order matches the file
            return String.format(template, app.toString(), context.toString());

        } catch (IOException e) {
            // Fallback inline prompt — ensures the app keeps working
            // even if the resource file is accidentally deleted
            throw new IllegalStateException(
                    "Failed to load prompt template from classpath. " +
                            "Ensure loan-evaluation.txt exists in resources/prompt-templates/", e);
        }
    }

    /**
     * Used when the LLM returns something unparseable.
     * Defaults to rejection to prevent unsafe approvals on parse failure.
     */
    private LoanDecisionResponse fallbackDecision(String raw) {
        // Fail safe: unparseable LLM output always defaults to rejection
        return new LoanDecisionResponse(false,
                "Could not parse AI response — defaulting to rejection. Raw: " + raw,
                List.of());
    }
}