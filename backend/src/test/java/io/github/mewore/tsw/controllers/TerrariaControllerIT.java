package io.github.mewore.tsw.controllers;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.mewore.tsw.config.TestConfig;
import io.github.mewore.tsw.config.security.AuthorityRoles;
import io.github.mewore.tsw.models.terraria.TModLoaderVersionViewModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceCreationModel;
import io.github.mewore.tsw.services.TerrariaService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@WebMvcTest(TerrariaController.class)
class TerrariaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TerrariaService terrariaService;

    @Captor
    private ArgumentCaptor<TerrariaInstanceCreationModel> creationModelCaptor;

    @Test
    void testGetTModLoaderVersions() throws Exception {
        final List<TModLoaderVersionViewModel> versions =
                Collections.singletonList(new TModLoaderVersionViewModel(1, "v0.11.8.1"));
        when(terrariaService.fetchTModLoaderVersions()).thenReturn(versions);
        mockMvc
                .perform(MockMvcRequestBuilders.get("/api/terraria/tmodloader/versions"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().string("[{\"releaseId\":1,\"version\":\"v0.11.8.1\"}]"));
    }

    @WithMockUser(authorities = {AuthorityRoles.MANAGE_HOSTS})
    @Test
    void testCreateTerrariaInstance() throws Exception {
        mockMvc
                .perform(MockMvcRequestBuilders
                        .post("/api/terraria/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "hostId": 1,
                                    "modLoaderReleaseId": 8,
                                    "terrariaServerArchiveUrl": "server-archive-url"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()));

        verify(terrariaService).createTerrariaInstance(creationModelCaptor.capture());
        final TerrariaInstanceCreationModel model = creationModelCaptor.getValue();
        assertEquals(1, model.getHostId());
        assertEquals(8, model.getModLoaderReleaseId());
        assertEquals("server-archive-url", model.getTerrariaServerArchiveUrl());
    }

    @Test
    void testCreateTerrariaInstance_anonymous() throws Exception {
        mockMvc
                .perform(MockMvcRequestBuilders.post("/api/terraria/instances"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    @WithMockUser
    @Test
    void testCreateTerrariaInstance_noPermissions() throws Exception {
        mockMvc
                .perform(MockMvcRequestBuilders
                        .post("/api/terraria/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.FORBIDDEN.value()));
    }
}