package com.thomassabu.transactionmonitor.exception;

/**
 * Thrown when a requested resource (e.g. transaction by id) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }
}
