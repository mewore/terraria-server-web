package io.github.mewore.tsw.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mewore.tsw.config.ConfigConstants;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.services.HostService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping(ConfigConstants.API_ROOT + "/hosts")
@ResponseBody
public class HostController {

    private final HostService hostService;

    @GetMapping
    public List<HostEntity> getHosts() {
        return hostService.getAllHosts();
    }
}
