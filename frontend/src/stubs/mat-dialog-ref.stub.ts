import { DialogPosition, MatDialogRef, MatDialogState, _MatDialogContainerBase } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';

export class MatDialogRefStub<C, O> implements Required<MatDialogRef<C, O>> {
    get _containerInstance(): _MatDialogContainerBase {
        throw new Error('Method not mocked.');
    }

    get id(): string {
        throw new Error('Method not mocked.');
    }

    get componentInstance(): C {
        throw new Error('Method not mocked.');
    }

    get disableClose(): boolean | undefined {
        throw new Error('Method not mocked.');
    }

    static withCloseValue<C, O>(closeValue?: O): MatDialogRef<C, O> {
        const dialog = new MatDialogRefStub<C, O>() as MatDialogRef<C, O>;
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

    updatePosition(_position?: DialogPosition): MatDialogRef<C, O> {
        throw new Error('Method not mocked.');
    }

    updateSize(_width?: string, _height?: string): MatDialogRef<C, O> {
        throw new Error('Method not mocked.');
    }

    addPanelClass(_classes: string | string[]): MatDialogRef<C, O> {
        throw new Error('Method not mocked.');
    }

    removePanelClass(_classes: string | string[]): MatDialogRef<C, O> {
        throw new Error('Method not mocked.');
    }

    getState(): MatDialogState {
        throw new Error('Method not mocked.');
    }
}
