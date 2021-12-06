package com.generic.springbootstartererrorhandler;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * - BindException – This exception is thrown when fatal binding errors occur.
     * - MethodArgumentNotValidException – This exception is thrown when an argument annotated with @Valid failed validation:
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

        logger.error("Bad request exception: {} headers: {}, status: {}, request: {}", ex, headers, status, request);

        List<ApiBadRequestException> badRequestExceptions = new ArrayList<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            var badRequestException = new ApiBadRequestException();
            var code = generateUniqueCodeHelper(error.getCode(), error.getDefaultMessage());
            badRequestException.setField(error.getField());
            badRequestException.setMessage(error.getDefaultMessage());
            badRequestException.setCode(code);
            badRequestExceptions.add(badRequestException);
        }

        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            var badRequestException = new ApiBadRequestException();
            var code = generateUniqueCodeHelper(error.getCode(), error.getDefaultMessage());
            badRequestException.setField(error.getObjectName());
            badRequestException.setMessage(error.getDefaultMessage());
            badRequestException.setCode(code);
            badRequestExceptions.add(badRequestException);
        }

        return handleExceptionInternal(ex, badRequestExceptions, headers, HttpStatus.BAD_REQUEST, request);

    }

    UUID generateUniqueCodeHelper(String code, String message) {
        var codeBytes = code.getBytes();
        var messageBytes = message.getBytes();
        var bothBytes = ArrayUtils.addAll(codeBytes, messageBytes);
        return UUID.nameUUIDFromBytes(bothBytes);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception ex, @Nullable Object body, @NonNull HttpHeaders headers, HttpStatus status, @NonNull WebRequest request) {
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * - MissingServletRequestPartException – This exception is thrown when the part of a multipart request is not found.
     * - MissingServletRequestParameterException – This exception is thrown when the request is missing a parameter:
     */
    @Override
    protected ResponseEntity handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String error = ex.getParameterName() + " parameter is missing";
        return handleExceptionInternal(ex, error, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
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
        logger.error("TypeMismatchException or MethodArgumentTypeMismatchException error ex: {} request: {}", ex, request);
        String error = ex.getName() + " should be of type " + ex.getRequiredType().getName();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllUncaughtException(Exception exception, WebRequest request) {
        logger.error("Unknown error occurred ex: {} request: {}", exception, request);

        var apiError = new ApiError();

        if (exception instanceof ResponseStatusException) {
            var status = ((ResponseStatusException) exception).getStatus();
            var reason = ((ResponseStatusException) exception).getReason();
            apiError.setStatus(status.value());
            apiError.setMessage(reason);
            return handleExceptionInternal(exception, apiError, new HttpHeaders(), status, request);
        }

        apiError.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        apiError.setMessage(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        return handleExceptionInternal(exception, apiError, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logger.error("Message Not Readable exception: {} headers: {}, status: {}, request: {}", ex, headers, status, request);
        var apiError = new ApiError();
        apiError.setStatus(status.value());
        apiError.setMessage(ex.getMessage());
        return handleExceptionInternal(ex, apiError, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

}
