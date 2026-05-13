package com.example.loanmcp.repository;

import com.example.loanmcp.domain.LoanDecision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanDecisionRepository extends JpaRepository<LoanDecision, Long> {
}
