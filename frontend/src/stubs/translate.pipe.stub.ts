import { Pipe, PipeTransform } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ImperfectStub } from './imperfect-stub';
import { EnUsTranslateServiceStub } from './translate.service.stub';

@Pipe({ name: 'translate' })
export class EnUsTranslatePipeStub extends ImperfectStub<TranslatePipe> implements PipeTransform {
    private readonly translateService = new EnUsTranslateServiceStub().masked();

    transform(query: string, interpolateParams?: { [key: string]: any }): string {
        return this.translateService.instant(query, interpolateParams);
    }
}

@Pipe({ name: 'translate' })
export class NoLanguageTranslatePipeStub extends ImperfectStub<TranslatePipe> implements PipeTransform {
    transform(query: string, _interpolateParams?: { [key: string]: any }): string {
        return `translated(${query})`;
    }
}
