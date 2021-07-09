package io.github.mewore.tsw.models.auth;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SignupModel {

    private final @NotBlank String username;

    private final @NotBlank String password;
}
