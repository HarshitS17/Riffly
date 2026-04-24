package com.riffly.exception;

import org.springframework.http.HttpStatus;

public class RifflyException extends RuntimeException {

    private final HttpStatus status;

    public RifflyException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static RifflyException notFound(String entity, Long id) {
        return new RifflyException(entity + " not found with id: " + id, HttpStatus.NOT_FOUND);
    }

    public static RifflyException conflict(String message) {
        return new RifflyException(message, HttpStatus.CONFLICT);
    }

    public static RifflyException badRequest(String message) {
        return new RifflyException(message, HttpStatus.BAD_REQUEST);
    }

    public static RifflyException fileNotFound(String path) {
        return new RifflyException("Audio file not found: " + path, HttpStatus.NOT_FOUND);
    }
}
