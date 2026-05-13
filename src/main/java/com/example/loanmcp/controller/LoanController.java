package com.example.loanmcp.controller;
import com.example.loanmcp.model.LoanDecisionResponse;
import com.example.loanmcp.service.LoanAgentService;
import com.example.loanmcp.domain.LoanApplication;
import com.example.loanmcp.audit.DecisionAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loan")
public class LoanController {

    private final LoanAgentService agentService;
    private final DecisionAuditService auditService;

    public LoanController(LoanAgentService agentService,
                          DecisionAuditService auditService) {
        this.agentService = agentService;
        this.auditService = auditService;
    }

    @PostMapping("/apply")
    public ResponseEntity<LoanDecisionResponse> apply(@RequestBody LoanApplication app) {
        LoanDecisionResponse decision = agentService.evaluateLoan(app);
        HttpStatus status = decision.isApproved()
                ? HttpStatus.OK
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(decision);
    }

    @GetMapping("/audit")
    public Object audit() {

        return auditService.getAll();
    }
}