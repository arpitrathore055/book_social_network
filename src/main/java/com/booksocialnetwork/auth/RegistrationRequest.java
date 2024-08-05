package com.booksocialnetwork.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class RegistrationRequest {

    @NotNull(message = "FirstName is mandatory")
    private String firstname;

    @NotNull(message = "LastName is mandatory")
    private String lastname;

    @Email(message = "Email is not formatted")
    @NotNull(message = "Email is mandatory")
    private String email;

    @NotNull(message = "Password is mandatory")
    @Size(min=8,message = "Password should be 8 characters long minimum")
    private String password;

}
