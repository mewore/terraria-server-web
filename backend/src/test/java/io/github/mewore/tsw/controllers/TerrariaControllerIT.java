package io.github.mewore.tsw.controllers;

import java.util.Collections;
import java.util.List;

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
import io.github.mewore.tsw.models.terraria.TModLoaderVersionViewModel;
import io.github.mewore.tsw.services.terraria.TerrariaInstanceService;

import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@WebMvcTest(TerrariaController.class)
class TerrariaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TerrariaInstanceService terrariaInstanceService;

    @Test
    void testGetTModLoaderVersions() throws Exception {
        final List<TModLoaderVersionViewModel> versions =
                Collections.singletonList(new TModLoaderVersionViewModel(1, "v0.11.8.1"));
        when(terrariaInstanceService.fetchTModLoaderVersions()).thenReturn(versions);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/terraria/tmodloader/versions"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().string("[{\"releaseId\":1,\"version\":\"v0.11.8.1\"}]"));
    }

    @Test
    void testCreateTerrariaInstance_anonymous() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/terraria/instances"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.UNAUTHORIZED.value()));
    }
}