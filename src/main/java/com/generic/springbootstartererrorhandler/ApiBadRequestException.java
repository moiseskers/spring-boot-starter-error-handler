package com.generic.springbootstartererrorhandler;

import java.util.UUID;

public class ApiBadRequestException {

    public String field;
    public String message;
    public UUID code;

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


}
