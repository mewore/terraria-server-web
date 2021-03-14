package io.github.mewore.tsw.controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mewore.tsw.config.ConfigConstants;
import io.github.mewore.tsw.models.terraria.TModLoaderVersionViewModel;
import io.github.mewore.tsw.services.TerrariaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@RestController
@RequestMapping(ConfigConstants.API_ROOT + "/terraria")
@ResponseBody
class TerrariaController {

    private final TerrariaService terrariaService;

    @GetMapping(path = "/tmodloader/versions")
    List<TModLoaderVersionViewModel> getTModLoaderVersions() throws IOException {
        return terrariaService.fetchTModLoaderVersions();
    }
}
