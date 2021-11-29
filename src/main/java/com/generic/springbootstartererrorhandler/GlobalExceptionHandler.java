package com.generic.springbootstartererrorhandler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * - BindException – This exception is thrown when fatal binding errors occur.
     * - MethodArgumentNotValidException – This exception is thrown when an argument annotated with @Valid failed validation:
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName;
            try {
                fieldName = ((FieldError) error).getField();
            } catch (Exception e) {
                fieldName = error.getDefaultMessage();
            }
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * - MissingServletRequestPartException – This exception is thrown when the part of a multipart request is not found.
     * - MissingServletRequestParameterException – This exception is thrown when the request is missing a parameter:
     */
    @Override
    protected ResponseEntity handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String error = ex.getParameterName() + " parameter is missing";
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * - ConstraintViolationException – This exception reports the result of constraint violations:
     */
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(violation.getRootBeanClass().getName() + " " + violation.getPropertyPath() + ": " + violation.getMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * - TypeMismatchException – This exception is thrown when trying to set bean property with the wrong type.
     * - MethodArgumentTypeMismatchException – This exception is thrown when method argument is not the expected type:
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String error = ex.getName() + " should be of type " + ex.getRequiredType().getName();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * It's used in case you want yourself to throw a custom bad request.
     * For instance:
     * <p>
     * Map<String, String> badRequest  = new HashMap<>() {{
     * put("field1", "must not be blank!");
     * put("field2", "must not be blank either!");
     * }};
     * <p>
     * throw new BadRequestException(badRequest);
     * <p>
     * **Remember to use the BadRequestException object, it's the project defaults!
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(BadRequestException exception) {
        return new ResponseEntity(exception.errors, HttpStatus.BAD_REQUEST);
    }
}
