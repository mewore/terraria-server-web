package io.github.mewore.tsw.models.auth;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LoginModel {

    @NotBlank
    private final String username;

    @NotBlank
    private final String password;
}
