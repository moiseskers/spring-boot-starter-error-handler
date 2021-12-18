package com.generic.springbootstartererrorhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle HttpMediaTypeNotSupportedException. This one triggers when JSON is invalid as well.
     *
     * @param ex      HttpMediaTypeNotSupportedException
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logger.error("HttpMediaTypeNotSupportedException headers: {}, status: {}, request: {}, ex: {} ", headers, status, request, ex);
        ApiError apiError = new ApiError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase());
        return new ResponseEntity<>(apiError, apiError.getError());
    }

    /**
     * Handle MethodArgumentNotValidException. Triggered when an object fails @Valid validation.
     *
     * @param ex      the MethodArgumentNotValidException that is thrown when @Valid validation fails
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ApiError object
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logger.error("Bad request exception headers: {}, status: {}, request: {}, ex: {}", headers, status, request, ex);
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Validation error");
        apiError.addValidationErrors(ex.getBindingResult().getFieldErrors());
        apiError.addValidationError(ex.getBindingResult().getGlobalErrors());
        return handleExceptionInternal(ex, apiError, headers, apiError.getError(), request);
    }

    /**
     * Handle MissingServletRequestParameterException. Triggered when a 'required' request parameter is missing.
     *
     * @param ex      MissingServletRequestParameterException
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ResponseEntity object
     */
    @Override
    protected ResponseEntity handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logger.error("MissingServletRequestParameterException error headers {} , status {} , request {}, ex {}", headers, status, request, ex);
        String error = ex.getParameterName() + " parameter is missing";
        var apiError = new ApiError(HttpStatus.BAD_REQUEST, error);
        return handleExceptionInternal(ex, apiError, headers, apiError.getError(), request);
    }

    /**
     * Handles javax.validation.ConstraintViolationException. Thrown when @Validated fails.
     *
     * @param ex the ConstraintViolationException
     * @return the ApiError object
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Validation error");
        apiError.addValidationErrors(ex.getConstraintViolations());
        return new ResponseEntity<>(apiError, apiError.getError());
    }


    /**
     * Handles EntityNotFoundException. Created to encapsulate errors with more detail than javax.persistence.EntityNotFoundException.
     *
     * @param ex the EntityNotFoundException
     * @return the ApiError object
     */
    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException ex) {
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getMessage());
        return new ResponseEntity<>(apiError, apiError.getError());
    }

    /**
     * Handle DataIntegrityViolationException, inspects the cause for different DB causes.
     *
     * @param ex the DataIntegrityViolationException
     * @return the ApiError object
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {

        if (ex.getCause() instanceof ConstraintViolationException) {
            logger.error("DataIntegrityViolationException, database error request {}, ex: {}", request, ex);
            var apiError = new ApiError(HttpStatus.CONFLICT, "Database error");
            return new ResponseEntity<>(apiError, apiError.getError());
        }

        logger.error("DataIntegrityViolationException request {}, ex: {}", request, ex);

        var apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Data integrity violation");
        return new ResponseEntity<>(apiError, apiError.getError());
    }

    /**
     * Handle MethodArgumentTypeMismatchException and TypeMismatchException
     *
     * @param ex the Exception
     * @return the ApiError object
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        logger.error("TypeMismatchException or MethodArgumentTypeMismatchException error request: {}, ex: {}", request, ex);
        var message = String.format("The parameter '%s' of value '%s' could not be converted to type '%s'", ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());
        var apiError = new ApiError(HttpStatus.BAD_REQUEST, message);
        return new ResponseEntity<>(apiError, apiError.getError());
    }

    /**
     * Handle Exception, handle generic Exception.class
     *
     * @param ex the Exception
     * @return the ApiError object
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllUncaughtException(Exception ex, WebRequest request) {

        logger.error("Unknown error occurred request: {}, ex: {}", request, ex);

        if (ex instanceof ResponseStatusException) {
            var status = ((ResponseStatusException) ex).getStatus();
            var reason = ((ResponseStatusException) ex).getReason();
            var apiError = new ApiError(status, reason);
            apiError.setError(status);
            apiError.setMessage(reason);
            return handleExceptionInternal(ex, apiError, new HttpHeaders(), status, request);
        }

        var apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        return handleExceptionInternal(ex, apiError, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Handle HttpMessageNotWritableException.
     *
     * @param ex      HttpMessageNotWritableException
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logger.error("Message Not Readable exception, headers: {}, status: {}, request: {}, ex: {}", headers, status, request, ex);
        var apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing JSON output");
        return new ResponseEntity<>(apiError, apiError.getError());
    }

    /**
     * Handle NoHandlerFoundException.
     *
     * @param ex
     * @param headers
     * @param status
     * @param request
     * @return
     */
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logger.error("Message Not Readable exception, headers: {}, status: {}, request: {}, ex: {}", headers, status, request, ex);
        var message = String.format("Could not find the %s method for URL %s", ex.getHttpMethod(), ex.getRequestURL());
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, message);
        return new ResponseEntity<>(apiError, apiError.getError());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logger.error("Message Not Readable exception, headers: {}, status: {}, request: {}, ex", headers, status, request, ex);
        var apiError = new ApiError(HttpStatus.BAD_REQUEST, "Malformed JSON request");
        return handleExceptionInternal(ex, apiError, headers, apiError.getError(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, @NonNull WebRequest request) {
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

}
