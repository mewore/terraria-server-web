package io.github.mewore.tsw.controllers;

import java.util.Collections;

import org.junit.jupiter.api.Test;
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
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventType;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceEventRepository;
import io.github.mewore.tsw.services.terraria.TerrariaInstanceService;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@WebMvcTest(TerrariaInstanceController.class)
class TerrariaInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TerrariaInstanceService terrariaInstanceService;

    @MockBean
    private TerrariaInstanceEventRepository terrariaInstanceEventRepository;

    @Test
    void testGetInstanceDetails() throws Exception {
        final TerrariaInstanceEntity instance = makeInstance();
        when(terrariaInstanceService.getInstance(8L)).thenReturn(instance);

        final TerrariaInstanceEventEntity event = TerrariaInstanceEventEntity.builder()
                .id(1L)
                .instance(instance)
                .content("text")
                .type(TerrariaInstanceEventType.OUTPUT)
                .build();
        when(terrariaInstanceEventRepository.findTop100ByInstanceOrderByIdDesc(instance)).thenReturn(
                Collections.singletonList(event));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/terraria/instances/8"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()));
        verify(terrariaInstanceService, only()).getInstance(8L);
        verify(terrariaInstanceEventRepository, only()).findTop100ByInstanceOrderByIdDesc(instance);
    }

    @WithMockUser(authorities = {AuthorityRoles.MANAGE_TERRARIA})
    @Test
    void testUpdateInstance() throws Exception {
        final TerrariaInstanceEntity instance = makeInstance();
        when(terrariaInstanceService.updateInstance(eq(8L), any())).thenReturn(instance);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/terraria/instances/8")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()));
    }

    @WithMockUser
    @Test
    void testUpdateInstance_noPermissions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/terraria/instances/8")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(MockMvcResultMatchers.status().is(HttpStatus.FORBIDDEN.value()));
        verify(terrariaInstanceService, never()).updateInstance(anyLong(), any());
    }
}