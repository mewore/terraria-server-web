import { Injectable, OnDestroy } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Observable, Subject } from 'rxjs';

@Injectable()
export class MatDialogStub implements Required<MatDialog>, OnDestroy {
    get afterAllClosed(): Observable<void> {
        throw new Error('Method not mocked.');
    }

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
