export class ImperfectStub<T> {
    masked(): T {
        return this as unknown as T;
    }
}
