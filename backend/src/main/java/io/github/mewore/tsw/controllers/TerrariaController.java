package io.github.mewore.tsw.controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mewore.tsw.config.ConfigConstants;
import io.github.mewore.tsw.config.security.AuthorityRoles;
import io.github.mewore.tsw.exceptions.IncorrectUrlException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.terraria.TModLoaderVersionViewModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceCreationModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.services.TerrariaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RestController
@RequestMapping(ConfigConstants.API_ROOT + "/terraria")
@ResponseBody
class TerrariaController {

    private final TerrariaService terrariaService;

    @GetMapping(path = "/tmodloader/versions")
    List<TModLoaderVersionViewModel> getTModLoaderVersions() throws IOException {
        return terrariaService.fetchTModLoaderVersions();
    }

    @Secured({AuthorityRoles.MANAGE_HOSTS})
    @PostMapping(path = "/instances")
    TerrariaInstanceEntity createTerrariaInstance(@RequestBody final TerrariaInstanceCreationModel creationModel)
            throws IOException, NotFoundException, IncorrectUrlException {
        return terrariaService.createTerrariaInstance(creationModel);
    }
}
