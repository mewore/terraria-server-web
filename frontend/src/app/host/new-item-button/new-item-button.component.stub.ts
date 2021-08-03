import { Component, EventEmitter, Input, Output } from '@angular/core';
import { AccountTypeEntity } from 'src/generated/backend';

@Component({
    selector: 'tsw-new-item-button',
    template: '',
})
export class NewItemButtonStubComponent {
    @Input()
    requiredPermissions?: Array<Exclude<keyof AccountTypeEntity, 'id'>>;

    @Output()
    press = new EventEmitter<void>();
}
