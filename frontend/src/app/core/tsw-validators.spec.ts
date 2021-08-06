import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { TswValidators } from './tsw-validators';

describe('TswValidators', () => {
    const makeControl = (value?: string): AbstractControl => ({ value } as AbstractControl);

    const makeNumberControl = (value?: number): AbstractControl => ({ value } as AbstractControl);

    describe('notBlank', () => {
        describe('when there is a non-blank value', () => {
            it('should not return an error', () => {
                expect(TswValidators.notBlank(makeControl(' value '))).toBeNull();
            });
        });

        describe('when there is no value', () => {
            it('should return an error', () => {
                expect(TswValidators.notBlank(makeControl())).toEqual({ blank: true });
            });
        });

        describe('when there is a blank value', () => {
            it('should return an error', () => {
                expect(TswValidators.notBlank(makeControl('     '))).toEqual({ blank: true });
            });
        });
    });

    describe('fileExtension', () => {
        describe('when there is a value with the expected extension', () => {
            it('should not return an error', () => {
                expect(TswValidators.fileExtension('ext')(makeControl('file.ext'))).toBeNull();
            });
        });

        describe('when there is a value with a different type', () => {
            it('should not return an error', () => {
                expect(TswValidators.fileExtension('ext')(makeNumberControl(10))).toBeNull();
            });
        });

        describe('when there is no value', () => {
            it('should not return an error', () => {
                expect(TswValidators.fileExtension('ext')(makeControl())).toBeNull();
            });
        });

        describe('when there is an empty value', () => {
            it('should return an error', () => {
                expect(TswValidators.fileExtension('ext')(makeControl(''))).toEqual({
                    fileExtension: true,
                    'fileExtension:ext': true,
                });
            });
        });

        describe('when there is a value without an extension', () => {
            it('should return an error', () => {
                expect(TswValidators.fileExtension('ext')(makeControl('file.otherExt'))).toEqual({
                    fileExtension: true,
                    'fileExtension:ext': true,
                });
            });
        });
    });

    describe('terrariaUrl', () => {
        describe('when there is an HTTP Terraria hostname value', () => {
            it('should not return an error', () => {
                expect(TswValidators.terrariaUrl(makeControl('http://terraria.org/a'))).toBeNull();
            });
        });

        describe('when there is an HTTPS Terraria hostname value', () => {
            it('should not return an error', () => {
                expect(TswValidators.terrariaUrl(makeControl('https://terraria.org/a'))).toBeNull();
            });
        });

        describe('when there is a value with a different type', () => {
            it('should not return an error', () => {
                expect(TswValidators.terrariaUrl(makeNumberControl(10))).toBeNull();
            });
        });

        describe('when there is no value', () => {
            it('should not return an error', () => {
                expect(TswValidators.terrariaUrl(makeControl())).toBeNull();
            });
        });

        describe('when there is an empty value', () => {
            it('should return an error', () => {
                expect(TswValidators.terrariaUrl(makeControl(''))).toEqual({ terrariaUrl: true });
            });
        });

        describe('when there is a value that is not a Terraria URL', () => {
            it('should return an error', () => {
                expect(TswValidators.terrariaUrl(makeControl('invalidUrl'))).toEqual({ terrariaUrl: true });
            });
        });
    });

    describe('noDuplicates', () => {
        describe('with a set', () => {
            let duplicateValueSet: Set<string>;
            let validator: ValidatorFn;

            beforeEach(() => {
                duplicateValueSet = new Set<string>(['a']);
                validator = TswValidators.noDuplicates(duplicateValueSet);
            });

            describe('when there are no conflicts ', () => {
                it('should not return an error', () => {
                    expect(validator(makeControl('b'))).toBeNull();
                });
            });

            describe('when there are conflicts ', () => {
                it('should return an error', () => {
                    expect(validator(makeControl('a'))).toEqual({ duplicate: true });
                });
            });

            describe('when the set is modified to include the value', () => {
                beforeEach(() => {
                    duplicateValueSet.add('b');
                });

                it('should not return an error', () => {
                    expect(validator(makeControl('b'))).toEqual({ duplicate: true });
                });
            });
        });

        describe('with a set', () => {
            let duplicateValueArray: string[];
            let validator: ValidatorFn;

            beforeEach(() => {
                duplicateValueArray = ['a'];
                validator = TswValidators.noDuplicates(duplicateValueArray);
            });

            describe('when there are no conflicts ', () => {
                it('should not return an error', () => {
                    expect(validator(makeControl('b'))).toBeNull();
                });
            });

            describe('when there are conflicts ', () => {
                it('should return an error', () => {
                    expect(validator(makeControl('a'))).toEqual({ duplicate: true });
                });
            });

            describe('when the array is modified to include the value', () => {
                beforeEach(() => {
                    duplicateValueArray.push('b');
                });

                it('should not return an error', () => {
                    expect(validator(makeControl('b'))).toBeNull();
                });
            });
        });
    });
});
