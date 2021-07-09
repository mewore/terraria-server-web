package io.github.mewore.tsw.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceDefinitionModel;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.HostService;
import io.github.mewore.tsw.services.terraria.TerrariaInstanceService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@WebMvcTest(HostController.class)
class HostControllerIT {

    private static final TerrariaInstanceDefinitionModel INSTANCE_DEFINITION_MODEL =
            new TerrariaInstanceDefinitionModel("Terraria Instance", 8, "server-archive-url");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @MockBean
    private TerrariaWorldRepository terrariaWorldRepository;

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
     * Test {@link HostController#getHost(long)}}.
     */
    @Test
    void testGetHost() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/hosts/1"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()));
        verify(hostService, only()).getHost(1);
    }

    /**
     * Test {@link HostController#getHost(long)}}.
     */
    @Test
    void testGetHost_notFound() throws Exception {
        when(hostService.getHost(1L)).thenThrow(new NotFoundException(""));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/hosts/1"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND.value()));
    }

    /**
     * Test {@link HostController#getHostInstances(long)}.
     */
    @Test
    void testGetHostInstances() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/hosts/1/instances"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()));
        verify(terrariaInstanceRepository, only()).findByHostIdOrderByIdAsc(1);
    }

    /**
     * Test {@link HostController#createTerrariaInstance(long, TerrariaInstanceDefinitionModel)}.
     */
    @WithMockUser(authorities = {AuthorityRoles.MANAGE_HOSTS})
    @Test
    void testCreateTerrariaInstance() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/hosts/1/instances")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writer().writeValueAsString(INSTANCE_DEFINITION_MODEL)))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
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
                .content(new ObjectMapper().writer().writeValueAsString(INSTANCE_DEFINITION_MODEL)))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.FORBIDDEN.value()));
    }

    /**
     * Test {@link HostController#getHostWorlds(long)}.
     */
    @Test
    void testGetHostWorlds() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/hosts/1/worlds"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()));
        verify(terrariaWorldRepository, only()).findByHostIdOrderByIdAsc(1);
    }
}
