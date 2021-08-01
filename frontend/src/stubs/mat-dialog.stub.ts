import { Injectable, OnDestroy } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { ImperfectStub } from './imperfect-stub';

@Injectable()
export class MatDialogStub extends ImperfectStub<MatDialog> implements OnDestroy {
    get openDialogs(): MatDialogRef<any, any>[] {
        throw new Error('Method not mocked.');
    }

    get afterOpened(): Subject<MatDialogRef<any, any>> {
        throw new Error('Method not mocked.');
    }

    _getAfterAllClosed(): Subject<void> {
        throw new Error('Method not mocked.');
    }

    open<T, R>(_template: any, _config?: any): MatDialogRef<T, R> {
        throw new Error('Method not mocked.');
    }

    closeAll(): void {
        throw new Error('Method not mocked.');
    }

    getDialogById(_id: string): MatDialogRef<any, any> | undefined {
        throw new Error('Method not mocked.');
    }

    ngOnDestroy(): void {
        throw new Error('Method not mocked.');
    }
}
