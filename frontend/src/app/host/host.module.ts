import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatRippleModule } from '@angular/material/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { CreateTerrariaInstanceDialogComponent } from './create-terraria-instance-dialog/create-terraria-instance-dialog.component';
import {
    CreateTerrariaInstanceDialogService,
    CreateTerrariaInstanceDialogServiceImpl,
} from './create-terraria-instance-dialog/create-terraria-instance-dialog.service';
import { HostInfoPageComponent } from './host-info-page/host-info-page.component';
import { HostListItemComponent } from './host-list-item/host-list-item.component';
import { HostListPageComponent } from './host-list-page/host-list-page.component';
import { HostRoutingModule } from './host-routing.module';
import { NewItemButtonComponent } from './new-item-button/new-item-button.component';
import { TerrariaInstanceListItemComponent } from './terraria-instance-list-item/terraria-instance-list-item.component';
import { TerrariaWorldListItemComponent } from './terraria-world-list-item/terraria-world-list-item.component';

@NgModule({
    declarations: [
        CreateTerrariaInstanceDialogComponent,
        HostInfoPageComponent,
        HostListItemComponent,
        HostListPageComponent,
        TerrariaInstanceListItemComponent,
        TerrariaWorldListItemComponent,
        NewItemButtonComponent,
    ],
    imports: [
        CommonModule,
        HostRoutingModule,
        MatButtonModule,
        MatDialogModule,
        MatDividerModule,
        MatFormFieldModule,
        MatRippleModule,
        MatIconModule,
        MatInputModule,
        MatListModule,
        MatProgressBarModule,
        MatProgressSpinnerModule,
        MatSelectModule,
        MatTooltipModule,
        ReactiveFormsModule,
        TranslateModule.forChild(),
    ],
    providers: [{ provide: CreateTerrariaInstanceDialogService, useClass: CreateTerrariaInstanceDialogServiceImpl }],
})
export class HostModule {}
