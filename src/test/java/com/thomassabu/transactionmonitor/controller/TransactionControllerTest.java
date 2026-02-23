package com.thomassabu.transactionmonitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thomassabu.transactionmonitor.model.Transaction;
import com.thomassabu.transactionmonitor.model.TransactionRequest;
import com.thomassabu.transactionmonitor.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.thomassabu.transactionmonitor.exception.GlobalExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    private ObjectMapper objectMapper;
    private static final LocalDateTime SAMPLE_TIME = LocalDateTime.of(2025, 2, 22, 10, 0);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private Transaction sampleTransaction() {
        Transaction t = new Transaction("user1", 100.0, SAMPLE_TIME, 50);
        t.setId(1L);
        return t;
    }

    private TransactionRequest sampleRequest() {
        TransactionRequest r = new TransactionRequest();
        r.setUserId("user1");
        r.setAmount(100.0);
        r.setTimestamp(SAMPLE_TIME);
        r.setRiskScore(50);
        return r;
    }

    @Nested
    @DisplayName("GET /transactions")
    class ListAll {
        @Test
        void returns200AndListOfTransactions() throws Exception {
            Transaction t = sampleTransaction();
            when(transactionService.findAll()).thenReturn(List.of(t));

            mockMvc.perform(get("/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].userId").value("user1"))
                    .andExpect(jsonPath("$[0].amount").value(100.0))
                    .andExpect(jsonPath("$[0].riskScore").value(50));

            verify(transactionService).findAll();
        }

        @Test
        void returnsEmptyArrayWhenNoTransactions() throws Exception {
            when(transactionService.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(transactionService).findAll();
        }
    }

    @Nested
    @DisplayName("GET /transactions/{id}")
    class GetOne {
        @Test
        void returns200AndTransactionWhenExists() throws Exception {
            Transaction t = sampleTransaction();
            when(transactionService.findById(1L)).thenReturn(t);

            mockMvc.perform(get("/transactions/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.userId").value("user1"));

            verify(transactionService).findById(1L);
        }

        @Test
        void returns404WhenNotExists() throws Exception {
            when(transactionService.findById(999L))
                    .thenThrow(new com.thomassabu.transactionmonitor.exception.ResourceNotFoundException("Transaction", 999L));

            mockMvc.perform(get("/transactions/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Transaction not found with id: 999"));

            verify(transactionService).findById(999L);
        }
    }

    @Nested
    @DisplayName("POST /transactions")
    class Create {
        @Test
        void returns201AndCreatedTransaction() throws Exception {
            Transaction t = sampleTransaction();
            when(transactionService.create(any(TransactionRequest.class))).thenReturn(t);

            String body = objectMapper.writeValueAsString(sampleRequest());

            mockMvc.perform(post("/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.userId").value("user1"));

            verify(transactionService).create(any(TransactionRequest.class));
        }

        @Test
        void returns400WhenValidationFails() throws Exception {
            TransactionRequest invalid = sampleRequest();
            invalid.setUserId(null);
            invalid.setAmount(-1.0);
            String body = objectMapper.writeValueAsString(invalid);

            mockMvc.perform(post("/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").exists());

            verify(transactionService, org.mockito.Mockito.never()).create(any());
        }
    }

    @Nested
    @DisplayName("PUT /transactions/{id}")
    class Update {
        @Test
        void returns200AndUpdatedTransaction() throws Exception {
            Transaction t = sampleTransaction();
            t.setAmount(200.0);
            when(transactionService.update(eq(1L), any(TransactionRequest.class))).thenReturn(t);

            String body = objectMapper.writeValueAsString(sampleRequest());

            mockMvc.perform(put("/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(200.0));

            verify(transactionService).update(eq(1L), any(TransactionRequest.class));
        }

        @Test
        void returns404WhenNotExists() throws Exception {
            when(transactionService.update(eq(999L), any(TransactionRequest.class)))
                    .thenThrow(new com.thomassabu.transactionmonitor.exception.ResourceNotFoundException("Transaction", 999L));

            String body = objectMapper.writeValueAsString(sampleRequest());

            mockMvc.perform(put("/transactions/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());

            verify(transactionService).update(eq(999L), any(TransactionRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /transactions/{id}")
    class Delete {
        @Test
        void returns204WhenDeleted() throws Exception {
            mockMvc.perform(delete("/transactions/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(transactionService).deleteById(1L);
        }

        @Test
        void returns404WhenNotExists() throws Exception {
            org.mockito.Mockito.doThrow(new com.thomassabu.transactionmonitor.exception.ResourceNotFoundException("Transaction", 999L))
                    .when(transactionService).deleteById(999L);

            mockMvc.perform(delete("/transactions/999"))
                    .andExpect(status().isNotFound());

            verify(transactionService).deleteById(999L);
        }
    }

    @Nested
    @DisplayName("GET /transactions/high-risk")
    class HighRisk {
        @Test
        void returns200AndOnlyHighRiskTransactions() throws Exception {
            Transaction high = new Transaction("user2", 500.0, SAMPLE_TIME, 85);
            high.setId(2L);
            when(transactionService.findHighRisk()).thenReturn(List.of(high));

            mockMvc.perform(get("/transactions/high-risk"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].riskScore").value(85));

            verify(transactionService).findHighRisk();
        }
    }
}
