import { Component, Input, OnInit } from '@angular/core';
import { CreateTerrariaInstanceDialogService } from '../create-terraria-instance-dialog/create-terraria-instance-dialog.service';

@Component({
    selector: 'tsw-terraria-instance-card',
    templateUrl: './terraria-instance-card.component.html',
    styleUrls: ['./terraria-instance-card.component.sass'],
})
export class TerrariaInstanceCardComponent implements OnInit {
    @Input()
    newInstance?: boolean;

    constructor(private readonly createDialogService: CreateTerrariaInstanceDialogService) {}

    ngOnInit(): void {}

    onCreateClicked(): void {
        this.createDialogService.openDialog();
    }
}
