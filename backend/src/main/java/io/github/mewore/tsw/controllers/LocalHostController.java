package io.github.mewore.tsw.controllers;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mewore.tsw.config.ConfigConstants;
import io.github.mewore.tsw.services.LocalHostService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping(ConfigConstants.API_ROOT + "/hosts/local")
@ResponseBody
public class LocalHostController {

    private final LocalHostService localHostService;

    @GetMapping(path = "/uuid")
    public UUID getLocalHostUuid() {
        return localHostService.getHostUuid();
    }
}
