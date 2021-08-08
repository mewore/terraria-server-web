package io.github.mewore.tsw.controllers;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mewore.tsw.config.ConfigConstants;
import io.github.mewore.tsw.config.security.AuthorityRoles;
import io.github.mewore.tsw.exceptions.InvalidRequestException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceDetailsViewModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceUpdateModel;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceEventRepository;
import io.github.mewore.tsw.services.terraria.TerrariaInstanceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RestController
@RequestMapping(ConfigConstants.API_ROOT + "/terraria/instances")
@ResponseBody
class TerrariaInstanceController {

    private final TerrariaInstanceService terrariaInstanceService;

    private final TerrariaInstanceEventRepository terrariaInstanceEventRepository;

    @Transactional
    @GetMapping(path = "/{instanceId}")
    TerrariaInstanceDetailsViewModel getInstanceDetails(@PathVariable("instanceId") final long instanceId)
            throws NotFoundException {
        final TerrariaInstanceEntity instance = terrariaInstanceService.getInstance(instanceId);
        final List<TerrariaInstanceEventEntity> events =
                terrariaInstanceEventRepository.findTop100ByInstanceOrderByIdDesc(
                instance);
        Collections.reverse(events);
        return new TerrariaInstanceDetailsViewModel(instance, events);
    }

    @Secured({AuthorityRoles.MANAGE_TERRARIA})
    @PatchMapping(path = "/{instanceId}")
    TerrariaInstanceEntity updateInstance(@PathVariable("instanceId") final long instanceId,
            @RequestBody final @Valid TerrariaInstanceUpdateModel model)
            throws NotFoundException, InvalidRequestException {
        return terrariaInstanceService.updateInstance(instanceId, model);
    }
}
