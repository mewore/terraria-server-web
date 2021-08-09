package io.github.mewore.tsw.controllers;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mewore.tsw.config.ConfigConstants;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.file.FileDataEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RestController
@RequestMapping(ConfigConstants.API_ROOT + "/terraria/worlds")
@ResponseBody
class TerrariaWorldController {

    private final TerrariaWorldRepository terrariaWorldRepository;

    @GetMapping(path = "/{worldId}/data")
    ResponseEntity<Resource> getWorldData(@PathVariable("worldId") final long worldId) throws NotFoundException {
        final FileDataEntity data = terrariaWorldRepository.findByIdWithData(worldId)
                .orElseThrow(() -> new NotFoundException("There is no world with ID " + worldId))
                .getData();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + data.getName() + "\"")
                .body(new ByteArrayResource(data.getContent()));
    }
}