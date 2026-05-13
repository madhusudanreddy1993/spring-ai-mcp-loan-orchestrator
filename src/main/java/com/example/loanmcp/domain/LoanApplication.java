package com.example.loanmcp.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int age;
    private double income;
    private double existingLoanAmount;
    private int creditScore;

    @Override
    public String toString() {
        return "LoanApplication{" +
                "id='" + id + '\'' +
                ", age=" + age +
                ", income=" + income +
                ", existingLoanAmount=" + existingLoanAmount +
                ", creditScore=" + creditScore +
                '}';
    }
}
