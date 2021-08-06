package io.github.mewore.tsw.controllers;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.mewore.tsw.config.TestConfig;
import io.github.mewore.tsw.models.file.FileDataEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@WebMvcTest(TerrariaWorldController.class)
class TerrariaWorldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TerrariaWorldRepository terrariaWorldRepository;

    @Test
    void testGetWorldData() throws Exception {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getData()).thenReturn(FileDataEntity.builder().name("world.zip").content("data".getBytes()).build());
        when(terrariaWorldRepository.findByIdWithData(10L)).thenReturn(Optional.of(world));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/terraria/worlds/10/data"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().bytes("data".getBytes()))
                .andExpect(MockMvcResultMatchers.header()
                        .string("Content-Disposition", "attachment;filename=\"world.zip\""));
    }

    @Test
    void testGetWorldData_notFound() throws Exception {
        when(terrariaWorldRepository.findByIdWithData(10L)).thenReturn(Optional.empty());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/terraria/worlds/10/data"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND.value()));
    }
}