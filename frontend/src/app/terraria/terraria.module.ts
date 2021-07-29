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
import { RunServerDialogComponent } from './run-server-dialog/run-server-dialog.component';
import { SetInstanceModsDialogComponent } from './set-instance-mods-dialog/set-instance-mods-dialog.component';
import { TerrariaInstancePageComponent } from './terraria-instance-page/terraria-instance-page.component';
import { TerrariaRoutingModule } from './terraria-routing.module';

@NgModule({
    declarations: [TerrariaInstancePageComponent, SetInstanceModsDialogComponent, RunServerDialogComponent],
    imports: [
        CommonModule,
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
})
export class TerrariaModule {}
