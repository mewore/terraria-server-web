package io.github.mewore.tsw.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mewore.tsw.config.ConfigConstants;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.services.LocalHostService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping(ConfigConstants.API_ROOT + "/hosts/local")
@ResponseBody
public class LocalHostController {

    private final LocalHostService localHostService;

    @GetMapping
    public HostEntity getLocalHost() {
        return localHostService.getHost();
    }
}
