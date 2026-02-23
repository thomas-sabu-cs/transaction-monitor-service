package com.thomassabu.transactionmonitor.service;

import com.thomassabu.transactionmonitor.exception.ResourceNotFoundException;
import com.thomassabu.transactionmonitor.model.Transaction;
import com.thomassabu.transactionmonitor.model.TransactionRequest;
import com.thomassabu.transactionmonitor.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction transaction;
    private TransactionRequest request;
    private static final LocalDateTime SAMPLE_TIME = LocalDateTime.of(2025, 2, 22, 10, 0);

    @BeforeEach
    void setUp() {
        transaction = new Transaction("user1", 100.0, SAMPLE_TIME, 50);
        transaction.setId(1L);
        request = new TransactionRequest();
        request.setUserId("user1");
        request.setAmount(100.0);
        request.setTimestamp(SAMPLE_TIME);
        request.setRiskScore(50);
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {
        @Test
        void returnsAllTransactions() {
            List<Transaction> list = List.of(transaction);
            when(transactionRepository.findAll()).thenReturn(list);

            List<Transaction> result = transactionService.findAll();

            assertThat(result).hasSize(1).containsExactly(transaction);
            verify(transactionRepository).findAll();
        }

        @Test
        void returnsEmptyListWhenNoTransactions() {
            when(transactionRepository.findAll()).thenReturn(List.of());

            List<Transaction> result = transactionService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {
        @Test
        void returnsTransactionWhenExists() {
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

            Transaction result = transactionService.findById(1L);

            assertThat(result).isEqualTo(transaction);
            verify(transactionRepository).findById(1L);
        }

        @Test
        void throwsResourceNotFoundExceptionWhenNotExists() {
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.findById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Transaction not found with id: 999");

            verify(transactionRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {
        @Test
        void persistsAndReturnsTransaction() {
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            Transaction result = transactionService.create(request);

            assertThat(result.getUserId()).isEqualTo("user1");
            assertThat(result.getAmount()).isEqualTo(100.0);
            assertThat(result.getTimestamp()).isEqualTo(SAMPLE_TIME);
            assertThat(result.getRiskScore()).isEqualTo(50);
            assertThat(result.getId()).isEqualTo(1L);
            verify(transactionRepository).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {
        @Test
        void updatesAndReturnsTransactionWhenExists() {
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
            request.setAmount(200.0);
            request.setRiskScore(80);

            Transaction result = transactionService.update(1L, request);

            assertThat(result.getAmount()).isEqualTo(200.0);
            assertThat(result.getRiskScore()).isEqualTo(80);
            verify(transactionRepository).findById(1L);
            verify(transactionRepository).save(transaction);
        }

        @Test
        void throwsResourceNotFoundExceptionWhenNotExists() {
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.update(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(transactionRepository).findById(999L);
            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {
        @Test
        void deletesWhenExists() {
            when(transactionRepository.existsById(1L)).thenReturn(true);

            transactionService.deleteById(1L);

            verify(transactionRepository).existsById(1L);
            verify(transactionRepository).deleteById(1L);
        }

        @Test
        void throwsResourceNotFoundExceptionWhenNotExists() {
            when(transactionRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> transactionService.deleteById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(transactionRepository).existsById(999L);
            verify(transactionRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("findHighRisk")
    class FindHighRisk {
        @Test
        void returnsOnlyTransactionsWithRiskScoreGreaterThan70() {
            Transaction high = new Transaction("u2", 500.0, SAMPLE_TIME, 85);
            high.setId(2L);
            when(transactionRepository.findByRiskScoreGreaterThan(70)).thenReturn(List.of(high));

            List<Transaction> result = transactionService.findHighRisk();

            assertThat(result).hasSize(1).containsExactly(high);
            verify(transactionRepository).findByRiskScoreGreaterThan(70);
        }

        @Test
        void returnsEmptyWhenNoHighRisk() {
            when(transactionRepository.findByRiskScoreGreaterThan(70)).thenReturn(List.of());

            List<Transaction> result = transactionService.findHighRisk();

            assertThat(result).isEmpty();
        }
    }
}
