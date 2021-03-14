import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { CreateTerrariaInstanceDialogComponent } from './create-terraria-instance-dialog/create-terraria-instance-dialog.component';
import { HostInfoComponent } from './host-info/host-info.component';
import { HostListPageComponent } from './host-list-page/host-list-page.component';
import { HostRoutingModule } from './host-routing.module';
import { TerrariaInstanceCardComponent } from './terraria-instance-card/terraria-instance-card.component';

@NgModule({
    declarations: [
        HostInfoComponent,
        HostListPageComponent,
        TerrariaInstanceCardComponent,
        CreateTerrariaInstanceDialogComponent,
    ],
    imports: [
        CommonModule,
        TranslateModule.forChild(),
        HostRoutingModule,
        MatFormFieldModule,
        MatDividerModule,
        MatExpansionModule,
        MatIconModule,
        MatCardModule,
        MatButtonModule,
        MatTooltipModule,
        MatProgressSpinnerModule,
        MatDialogModule,
        MatProgressBarModule,
        ReactiveFormsModule,
        MatInputModule,
        MatSelectModule,
    ],
})
export class HostModule {}
