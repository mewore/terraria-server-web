package io.github.mewore.tsw.controllers;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mewore.tsw.config.ConfigConstants;
import io.github.mewore.tsw.config.security.AuthorityRoles;
import io.github.mewore.tsw.exceptions.InvalidRequestException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldFileEntity;
import io.github.mewore.tsw.services.terraria.TerrariaWorldService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RestController
@RequestMapping(ConfigConstants.API_ROOT + "/terraria/worlds")
@ResponseBody
class TerrariaWorldController {

    private final TerrariaWorldService terrariaWorldService;

    @GetMapping(path = "/{worldId}/data")
    ResponseEntity<Resource> getWorldData(@PathVariable("worldId") final long worldId) throws NotFoundException {
        final TerrariaWorldFileEntity data = terrariaWorldService.getWorldData(worldId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + data.getName() + "\"")
                .body(new ByteArrayResource(data.getContent()));
    }

    @Secured({AuthorityRoles.MANAGE_HOSTS})
    @DeleteMapping(path = "/{worldId}")
    void deleteWorld(@PathVariable("worldId") final long worldId) throws NotFoundException, InvalidRequestException {
        terrariaWorldService.deleteWorld(worldId);
    }
}
