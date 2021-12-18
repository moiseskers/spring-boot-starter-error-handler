package com.generic.springbootstartererrorhandler;

import com.generic.springbootstartererrorhandler.helpers.UUIDHelper;

import java.util.UUID;

public class ApiBadRequestException extends ApiSubError {

    public UUID code;
    public String field;
    public String message;
    public Object rejectedValue;
    public String objectName;

    public ApiBadRequestException(String field, String message, Object rejectedValue, String objectName) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
        this.objectName = objectName;
        this.code = generateCode();
    }

    public UUID generateCode() {
        try {
            return UUIDHelper.generateUniqueCodeHelper(this.field, this.message);
        } catch (Exception e) {
            return null;
        }
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public void setRejectedValue(Object rejectedValue) {
        this.rejectedValue = rejectedValue;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getCode() {
        return code;
    }

    public void setCode(UUID code) {
        this.code = code;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }
}
