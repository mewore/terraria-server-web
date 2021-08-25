package io.github.mewore.tsw.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.mewore.tsw.config.TestConfig;
import io.github.mewore.tsw.config.security.AuthorityRoles;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldFileEntity;
import io.github.mewore.tsw.services.terraria.TerrariaWorldService;

import static io.github.mewore.tsw.models.terraria.TerrariaWorldFactory.makeWorld;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@WebMvcTest(TerrariaWorldController.class)
class TerrariaWorldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TerrariaWorldService terrariaWorldService;

    @Test
    void testGetWorldData() throws Exception {
        final TerrariaWorldFileEntity worldFile = TerrariaWorldFileEntity.builder()
                .name("world.zip")
                .content("data".getBytes())
                .world(makeWorld())
                .build();
        when(terrariaWorldService.getWorldData(10L)).thenReturn(worldFile);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/terraria/worlds/10/data"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().bytes("data".getBytes()))
                .andExpect(MockMvcResultMatchers.header()
                        .string("Content-Disposition", "attachment;filename=\"world.zip\""));
    }

    @WithMockUser(authorities = {AuthorityRoles.MANAGE_HOSTS})
    @Test
    void testDeleteWorld() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/terraria/worlds/10"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()));
        verify(terrariaWorldService, only()).deleteWorld(10L);
    }

    @WithMockUser
    @Test
    void testDeleteWorld_noPermissions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/terraria/worlds/10"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.FORBIDDEN.value()));
        verify(terrariaWorldService, never()).deleteWorld(anyLong());
    }
}