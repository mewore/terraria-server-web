import { Pipe, PipeTransform } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ImperfectStub } from './imperfect-stub';

@Pipe({ name: 'translate' })
export class TranslatePipeStub extends ImperfectStub<TranslatePipe> implements PipeTransform {
    transform(value: any, ..._args: any[]): any {
        return value;
    }
}
