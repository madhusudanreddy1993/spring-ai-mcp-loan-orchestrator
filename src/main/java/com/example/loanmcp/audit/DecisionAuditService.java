package com.example.loanmcp.audit;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DecisionAuditService {

    private final List<DecisionAudit> audits = new ArrayList<>();

    public void log(DecisionAudit audit) {
        audits.add(audit);
    }

    public List<DecisionAudit> getAll() {
        return audits;
    }
}
