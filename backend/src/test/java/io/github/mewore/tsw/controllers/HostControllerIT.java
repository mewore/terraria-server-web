package io.github.mewore.tsw.controllers;

import java.util.Collections;
import java.util.UUID;

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
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.services.HostService;

import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@WebMvcTest(HostController.class)
class HostControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HostService hostService;

    @Test
    void testGetHosts() throws Exception {
        final HostEntity host = HostEntity
                .builder()
                .id(8L)
                .uuid(UUID.fromString("f7cda826-a3b6-4d0a-932f-4e384914b1c6"))
                .name("Host name")
                .build();
        when(hostService.getAllHosts()).thenReturn(Collections.singletonList(host));
        mockMvc
                .perform(MockMvcRequestBuilders.get("/api/hosts"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().json("""
                        [{
                            "id": 8,
                            "uuid": "f7cda826-a3b6-4d0a-932f-4e384914b1c6",
                            "alive": false,
                            "name": "Host name",
                            "url": null,
                            "terrariaInstanceDirectory": "%s",
                            "worlds": [],
                            "terrariaInstances": []
                        }]
                        """.formatted(host.getTerrariaInstanceDirectory().toUri()), true));
    }
}