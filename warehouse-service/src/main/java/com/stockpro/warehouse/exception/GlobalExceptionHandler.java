package com.stockpro.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String KEY_ERROR = "error";
    private static final String KEY_STATUS = "status";

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(KEY_ERROR, ex.getMessage());
        error.put(KEY_STATUS, "400");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // You can add more specific handlers here (e.g. for ResourceNotFound)
    
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(ProductNotFoundException ex) {

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put(KEY_ERROR, ex.getMessage());
        errorMap.put(KEY_STATUS, 404);

        return new ResponseEntity<>(errorMap, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CapacityExceededException.class)
    public ResponseEntity<Map<String, Object>> handleCapacity(CapacityExceededException ex) {

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put(KEY_ERROR, ex.getMessage());
        errorMap.put(KEY_STATUS, 400);

        return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
   }
}
