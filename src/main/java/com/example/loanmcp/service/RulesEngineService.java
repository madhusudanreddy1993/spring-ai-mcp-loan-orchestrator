package com.example.loanmcp.service;

import com.example.loanmcp.domain.LoanApplication;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RulesEngineService {

    private final KieContainer kieContainer;

    public RulesEngineService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public List<String> evaluate(LoanApplication app) {

        List<String> results = new ArrayList<>();
        KieSession session = null;

        try {
            // No named session — uses default session from programmatic build
            session = kieContainer.newKieSession();

            session.setGlobal("results", results);
            session.insert(app);
            session.fireAllRules();

        } catch (Exception e) {
            e.printStackTrace();
            results.add("RULE_ENGINE_ERROR: " + e.getMessage());
        } finally {
            // Always dispose in finally — prevents KieSession memory leaks
            if (session != null) {
                session.dispose();
            }
        }

        return results;
    }
}