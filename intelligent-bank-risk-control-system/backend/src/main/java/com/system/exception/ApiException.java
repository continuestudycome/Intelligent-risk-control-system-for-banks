package com.system.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final Integer code;

    public ApiException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
