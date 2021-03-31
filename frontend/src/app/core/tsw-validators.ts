import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export class TswValidators {
    static notBlank(control: AbstractControl): ValidationErrors | null {
        return ('' + control.value || '').trim() ? null : { blank: true };
    }

    static fileExtension(extension: string): ValidatorFn {
        const errorKey = 'fileExtension:' + extension;
        const desiredSuffix = '.' + extension;
        return (control) => {
            if (typeof control.value !== 'string') {
                return null;
            }
            if (!control.value.endsWith(desiredSuffix)) {
                const result: ValidationErrors = { fileExtension: true };
                result[errorKey] = true;
                return result;
            }
            return null;
        };
    }

    static terrariaUrl(control: AbstractControl): ValidationErrors | null {
        if (typeof control.value !== 'string') {
            return null;
        }
        if (!control.value.startsWith('http://terraria.org/') && !control.value.startsWith('https://terraria.org/')) {
            return { terrariaUrl: true };
        }
        return null;
    }
}
