package com.thomassabu.transactionmonitor.service;

import com.thomassabu.transactionmonitor.exception.ResourceNotFoundException;
import com.thomassabu.transactionmonitor.model.Transaction;
import com.thomassabu.transactionmonitor.model.TransactionRequest;
import com.thomassabu.transactionmonitor.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for transaction business logic.
 * Delegates persistence to {@link TransactionRepository}.
 */
@Service
public class TransactionService {

    private static final int HIGH_RISK_THRESHOLD = 70;

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Returns all transactions in the system.
     *
     * @return list of all transactions
     */
    @Transactional(readOnly = true)
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    /**
     * Finds a transaction by its id.
     *
     * @param id the transaction id
     * @return the transaction
     * @throws ResourceNotFoundException if no transaction exists with the given id
     */
    @Transactional(readOnly = true)
    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    /**
     * Creates a new transaction from the given request.
     *
     * @param request validated create request
     * @return the persisted transaction
     */
    @Transactional
    public Transaction create(TransactionRequest request) {
        Transaction transaction = new Transaction(
                request.getUserId(),
                request.getAmount(),
                request.getTimestamp(),
                request.getRiskScore()
        );
        return transactionRepository.save(transaction);
    }

    /**
     * Updates an existing transaction by id.
     *
     * @param id      the transaction id to update
     * @param request validated update request
     * @return the updated transaction
     * @throws ResourceNotFoundException if no transaction exists with the given id
     */
    @Transactional
    public Transaction update(Long id, TransactionRequest request) {
        Transaction existing = findById(id);
        existing.setUserId(request.getUserId());
        existing.setAmount(request.getAmount());
        existing.setTimestamp(request.getTimestamp());
        existing.setRiskScore(request.getRiskScore());
        return transactionRepository.save(existing);
    }

    /**
     * Deletes a transaction by id.
     *
     * @param id the transaction id to delete
     * @throws ResourceNotFoundException if no transaction exists with the given id
     */
    @Transactional
    public void deleteById(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction", id);
        }
        transactionRepository.deleteById(id);
    }

    /**
     * Returns transactions with risk score greater than 70 (high-risk).
     *
     * @return list of high-risk transactions
     */
    @Transactional(readOnly = true)
    public List<Transaction> findHighRisk() {
        return transactionRepository.findByRiskScoreGreaterThan(HIGH_RISK_THRESHOLD);
    }
}
