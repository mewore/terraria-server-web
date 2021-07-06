package io.github.mewore.tsw.controllers;

import javax.validation.Valid;
import java.util.List;

import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mewore.tsw.config.ConfigConstants;
import io.github.mewore.tsw.config.security.AuthorityRoles;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceDefinitionModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.services.HostService;
import io.github.mewore.tsw.services.terraria.TerrariaInstanceService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping(ConfigConstants.API_ROOT + "/hosts")
@ResponseBody
public class HostController {

    private final TerrariaInstanceService terrariaInstanceService;

    private final HostService hostService;

    @GetMapping(path = "/{hostId}")
    HostEntity getHost(@PathVariable final long hostId) throws NotFoundException {
        return hostService.getHost(hostId);
    }

    @GetMapping
    List<HostEntity> getHosts() {
        return hostService.getAllHosts();
    }

    @Secured({AuthorityRoles.MANAGE_HOSTS})
    @PostMapping(path = "/{hostId}/instances")
    TerrariaInstanceEntity createTerrariaInstance(@PathVariable final long hostId,
            @RequestBody final @Valid TerrariaInstanceDefinitionModel creationModel) throws NotFoundException {
        return terrariaInstanceService.defineTerrariaInstance(hostId, creationModel);
    }
}
