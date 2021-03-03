import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { HostInfoComponent } from './host-info/host-info.component';
import { HostListPageComponent } from './host-list-page/host-list-page.component';
import { HostRoutingModule } from './host-routing.module';

@NgModule({
    declarations: [HostInfoComponent, HostListPageComponent],
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
    ],
})
export class HostModule {}
