import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';
import { CoreModule } from '../core/core.module';
import { AuthenticationDialogComponent } from './authentication-dialog/authentication-dialog.component';
import { LogOutDialogComponent } from './log-out-dialog/log-out-dialog.component';

@NgModule({
    declarations: [AuthenticationDialogComponent, LogOutDialogComponent],
    imports: [
        CommonModule,
        TranslateModule.forChild(),
        ReactiveFormsModule,
        MatInputModule,
        MatDialogModule,
        MatButtonModule,
        MatProgressBarModule,
        CoreModule,
    ],
})
export class AuthenticationModule {}
