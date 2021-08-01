import { DialogPosition, MatDialogRef, MatDialogState, _MatDialogContainerBase } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import { ImperfectStub } from './imperfect-stub';

export class MatDialogRefStub<C, O> extends ImperfectStub<MatDialogRef<C, O>> {
    static withCloseValue<C, O>(closeValue?: O): MatDialogRef<C, O> {
        const dialog = new MatDialogRefStub<C, O>().masked();
        spyOn(dialog, 'afterClosed').and.returnValue(of(closeValue));
        return dialog;
    }

    close(_dialogResult?: O): void {
        throw new Error('Method not mocked.');
    }

    afterOpened(): Observable<void> {
        throw new Error('Method not mocked.');
    }

    afterClosed(): Observable<O | undefined> {
        throw new Error('Method not mocked.');
    }

    beforeClosed(): Observable<O | undefined> {
        throw new Error('Method not mocked.');
    }

    backdropClick(): Observable<MouseEvent> {
        throw new Error('Method not mocked.');
    }

    keydownEvents(): Observable<KeyboardEvent> {
        throw new Error('Method not mocked.');
    }

    updatePosition(_position?: DialogPosition): this {
        throw new Error('Method not mocked.');
    }

    updateSize(_width?: string, _height?: string): this {
        throw new Error('Method not mocked.');
    }

    addPanelClass(_classes: string | string[]): this {
        throw new Error('Method not mocked.');
    }

    removePanelClass(_classes: string | string[]): this {
        throw new Error('Method not mocked.');
    }

    getState(): MatDialogState {
        throw new Error('Method not mocked.');
    }
}
