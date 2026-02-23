package com.thomassabu.transactionmonitor.repository;

import com.thomassabu.transactionmonitor.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for Transaction entities.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Finds all transactions with risk score strictly greater than the given threshold.
     *
     * @param riskScore minimum exclusive threshold
     * @return list of high-risk transactions
     */
    List<Transaction> findByRiskScoreGreaterThan(Integer riskScore);
}
