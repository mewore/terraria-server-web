import { Type } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';

export const initComponent = async <T>(
    componentType: Type<T>,
    afterInstantiation?: (createdComponent: T) => void
): Promise<[ComponentFixture<T>, T]> => {
    let fixture: ComponentFixture<T> | undefined;
    fakeAsync(() => {
        fixture = TestBed.createComponent(componentType);
        if (afterInstantiation) {
            afterInstantiation(fixture.componentInstance);
        }
        refreshFixture(fixture);
    })();

    if (!fixture) {
        throw new Error('The fixture is not defined!');
    }

    await fixture.whenStable();
    await fixture.whenRenderingDone();
    return [fixture, fixture.componentInstance];
};

export const refreshFixture = (fixture: ComponentFixture<any>): void => {
    fixture.detectChanges();
    tick(1000);

    fixture.detectChanges();
    tick(1000);
};
