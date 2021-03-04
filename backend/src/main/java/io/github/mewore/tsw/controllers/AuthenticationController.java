package io.github.mewore.tsw.controllers;

import javax.validation.Valid;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mewore.tsw.config.security.AuthConfigConstants;
import io.github.mewore.tsw.exceptions.auth.InvalidCredentialsException;
import io.github.mewore.tsw.exceptions.auth.InvalidUsernameException;
import io.github.mewore.tsw.models.AccountTypeEntity;
import io.github.mewore.tsw.models.auth.LoginModel;
import io.github.mewore.tsw.models.auth.SessionViewModel;
import io.github.mewore.tsw.models.auth.SignupModel;
import io.github.mewore.tsw.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping(AuthConfigConstants.AUTH_API_URI)
@ResponseBody
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping({AuthConfigConstants.AUTH_LOGIN_ENDPOINT})
    public SessionViewModel logIn(@RequestBody @Valid final LoginModel loginModel) throws InvalidCredentialsException {
        return authenticationService.logIn(loginModel);
    }

    @PostMapping({AuthConfigConstants.AUTH_SIGN_UP_ENDPOINT})
    public SessionViewModel signUp(@RequestBody @Valid final SignupModel signupModel) throws InvalidUsernameException {
        return authenticationService.signUp(signupModel);
    }

    @Operation(security = {@SecurityRequirement(name = AuthConfigConstants.AUTH_SECURITY_REQUIREMENT)})
    @PostMapping({AuthConfigConstants.AUTH_LOG_OUT_ENDPOINT})
    public void logOut() throws InvalidCredentialsException {
        authenticationService.logOut(SecurityContextHolder.getContext().getAuthentication());
    }

    @Operation(security = {@SecurityRequirement(name = AuthConfigConstants.AUTH_SECURITY_REQUIREMENT)})
    @PostMapping({AuthConfigConstants.AUTH_PING_ENDPOINT})
    public AccountTypeEntity ping() throws InvalidCredentialsException {
        return authenticationService.getRole(SecurityContextHolder.getContext().getAuthentication());
    }
}
