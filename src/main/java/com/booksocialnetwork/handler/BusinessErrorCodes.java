package com.booksocialnetwork.handler;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public enum BusinessErrorCodes {
    NO_CODE(0,"No Code",HttpStatus.NOT_IMPLEMENTED),
    ACCOUNT_LOCKED(302,"User account is locked",HttpStatus.FORBIDDEN),
    INCORRECT_CURRENT_PASSWORD(300,"Current password is incorrect",HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_DOES_NOT_MATCH(301,"Current password does not match previous password",HttpStatus.BAD_REQUEST),
    ACCOUNT_DISABLED(303,"User account is disabled",HttpStatus.FORBIDDEN),
    BAD_CREDENTIALS(302,"Login and/or password is incorrect",HttpStatus.FORBIDDEN),
    ;
    @Getter
    private final int code;
    @Getter
    private final String description;
    @Getter
    private final HttpStatus httpStatus;

    BusinessErrorCodes(int code,String description,HttpStatus httpStatus){
        this.code=code;
        this.description=description;
        this.httpStatus=httpStatus;
    }

}
