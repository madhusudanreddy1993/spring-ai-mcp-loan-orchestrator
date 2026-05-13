package com.example.loanmcp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean approved;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private LocalDateTime decisionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status = WorkflowStatus.PENDING;


    @OneToOne
    @JoinColumn(name = "loan_application_id", referencedColumnName = "id")
    private LoanApplication loanApplication;

}
