package com.example.loanmcp.audit;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@RequiredArgsConstructor
public class DecisionAudit {

    private final String applicantId;
    private final boolean approved;
    private final String rawResponse;
    private Instant timestamp = Instant.now();



//    public DecisionAudit(String applicantId, boolean approved, String rawResponse) {
//        this.applicantId = applicantId;
//        this.approved = approved;
//        this.rawResponse = rawResponse;
//    }
}
