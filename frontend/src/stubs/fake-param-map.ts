import { ParamMap } from '@angular/router';

export class FakeParamMap implements ParamMap {
    constructor(private readonly rawParamMap: { [key: string]: string } = {}) {}

    has(name: string): boolean {
        return !!this.rawParamMap[name];
    }

    get(name: string): string | null {
        return this.rawParamMap[name];
    }

    getAll(name: string): string[] {
        return [this.rawParamMap[name]];
    }

    get keys(): string[] {
        return Object.keys(this.rawParamMap);
    }
}
