import { Pipe, PipeTransform } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ImperfectStub } from './imperfect-stub';
import { TranslateServiceStub } from './translate.service.stub';

@Pipe({ name: 'translate' })
export class TranslatePipeStub extends ImperfectStub<TranslatePipe> implements PipeTransform {
    private readonly translateService = new TranslateServiceStub().masked();

    transform(query: string, interpolateParams?: { [key: string]: any }): string {
        return this.translateService.instant(query, interpolateParams);
    }
}
