package io.github.mewore.tsw.controllers;

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
import io.github.mewore.tsw.models.terraria.TerrariaInstanceDefinitionModel;
import io.github.mewore.tsw.services.HostService;
import io.github.mewore.tsw.services.terraria.TerrariaInstanceService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

@Import(TestConfig.class)
@WebMvcTest(HostController.class)
class HostControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HostService hostService;

    @MockBean
    private TerrariaInstanceService terrariaInstanceService;

    @Captor
    private ArgumentCaptor<TerrariaInstanceDefinitionModel> instanceModelCaptor;

    /**
     * Test {@link HostController#getHosts()}.
     */
    @Test
    void testGetHosts() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/hosts"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()));
        verify(hostService, only()).getAllHosts();
    }

    /**
     * Test {@link HostController#createTerrariaInstance(long, TerrariaInstanceDefinitionModel)}.
     */
    @WithMockUser(authorities = {AuthorityRoles.MANAGE_HOSTS})
    @Test
    void testCreateTerrariaInstance() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/hosts/1/instances")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "instanceName": "Terraria Instance",
                            "modLoaderReleaseId": 8,
                            "terrariaServerArchiveUrl": "server-archive-url"
                        }
                        """)).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        verify(terrariaInstanceService, only()).defineTerrariaInstance(eq(1L), instanceModelCaptor.capture());

        final TerrariaInstanceDefinitionModel model = instanceModelCaptor.getValue();
        assertEquals("Terraria Instance", model.getInstanceName());
        assertEquals(8, model.getModLoaderReleaseId());
        assertEquals("server-archive-url", model.getTerrariaServerArchiveUrl());
    }

    @WithMockUser
    @Test
    void testCreateTerrariaInstance_noPermissions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/hosts/1/instances")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "instanceName": "Terraria Instance",
                            "modLoaderReleaseId": 8,
                            "terrariaServerArchiveUrl": "server-archive-url"
                        }
                        """)).andExpect(MockMvcResultMatchers.status().is(HttpStatus.FORBIDDEN.value()));
    }
}
