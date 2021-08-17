import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { TerrariaCoreModule } from '../terraria-core/terraria-core.module';
import { RunServerDialogComponent } from './run-server-dialog/run-server-dialog.component';
import { RunServerDialogService, RunServerDialogServiceImpl } from './run-server-dialog/run-server-dialog.service';
import { SetInstanceModsDialogComponent } from './set-instance-mods-dialog/set-instance-mods-dialog.component';
import {
    SetInstanceModsDialogService,
    SetInstanceModsDialogServiceImpl,
} from './set-instance-mods-dialog/set-instance-mods-dialog.service';
import { TerrariaInstancePageComponent } from './terraria-instance-page/terraria-instance-page.component';
import { TerrariaRoutingModule } from './terraria-routing.module';
import { CreateWorldDialogComponent } from './create-world-dialog/create-world-dialog.component';
import {
    CreateWorldDialogService,
    CreateWorldDialogServiceImpl,
} from './create-world-dialog/create-world-dialog.service';

@NgModule({
    declarations: [
        TerrariaInstancePageComponent,
        SetInstanceModsDialogComponent,
        RunServerDialogComponent,
        CreateWorldDialogComponent,
    ],
    imports: [
        CommonModule,
        TerrariaCoreModule,
        TerrariaRoutingModule,
        MatTooltipModule,
        MatButtonModule,
        MatCheckboxModule,
        MatDialogModule,
        MatFormFieldModule,
        MatIconModule,
        MatSelectModule,
        MatDialogModule,
        MatInputModule,
        ReactiveFormsModule,
        MatListModule,
        MatProgressBarModule,
        MatProgressSpinnerModule,
        TranslateModule.forChild(),
    ],
    providers: [
        { provide: SetInstanceModsDialogService, useClass: SetInstanceModsDialogServiceImpl },
        { provide: CreateWorldDialogService, useClass: CreateWorldDialogServiceImpl },
        { provide: RunServerDialogService, useClass: RunServerDialogServiceImpl },
    ],
})
export class TerrariaModule {}
