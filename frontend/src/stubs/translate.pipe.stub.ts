import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'translate' })
export class TranslatePipeStub implements PipeTransform {
    transform(value: any, ..._args: any[]): any {
        return value;
    }
}
