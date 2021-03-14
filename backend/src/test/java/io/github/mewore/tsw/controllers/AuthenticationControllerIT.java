package io.github.mewore.tsw.controllers;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.mewore.tsw.config.TestConfig;
import io.github.mewore.tsw.models.auth.SessionViewModel;
import io.github.mewore.tsw.services.AuthenticationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerIT {

    private static final String USERNAME = "username";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Captor
    private ArgumentCaptor<Authentication> authenticationCaptor;

    @Test
    void testLogIn() throws Exception {
        final UUID session = UUID.fromString("e0f245dc-e6e4-4f8a-982b-004cbb04e505");
        when(authenticationService.logIn(any())).thenReturn(new SessionViewModel(session, null));
        mockMvc
                .perform(MockMvcRequestBuilders.post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                            "username": "some-existing-username",
                            "password": "some-password"
                        }
                        """))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers
                        .content()
                        .string("{\"token\":\"e0f245dc-e6e4-4f8a-982b-004cbb04e505\",\"role\":null}"));
    }

    @Test
    void testSignUp() throws Exception {
        final UUID session = UUID.fromString("e0f245dc-e6e4-4f8a-982b-004cbb04e505");
        when(authenticationService.signUp(any())).thenReturn(new SessionViewModel(session, null));
        mockMvc
                .perform(MockMvcRequestBuilders.post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                            "username": "some-new-username",
                            "password": "some-password"
                        }
                        """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers
                        .content()
                        .string("{\"token\":\"e0f245dc-e6e4-4f8a-982b-004cbb04e505\",\"role\":null}"));
    }

    @WithMockUser(username = USERNAME)
    @Test
    void testLogOut() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/logout")).andExpect(MockMvcResultMatchers.status().isOk());
        verify(authenticationService, only()).logOut(authenticationCaptor.capture());
        assertEquals(USERNAME, ((UserDetails) authenticationCaptor.getValue().getPrincipal()).getUsername());
    }

    @Test
    void testLogOutUnauthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/logout"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @WithMockUser(username = USERNAME)
    @Test
    void testPing() throws Exception {
        when(authenticationService.getAuthenticatedAccountType(any())).thenReturn(null);
        mockMvc
                .perform(MockMvcRequestBuilders.post("/auth/ping"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(""));
        verify(authenticationService, only()).getAuthenticatedAccountType(authenticationCaptor.capture());
        assertEquals(USERNAME, ((UserDetails) authenticationCaptor.getValue().getPrincipal()).getUsername());
    }

    @Test
    void testPingUnauthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/ping"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}