/**
 * A stub directly of an implementation. It cannot extend the implementation and it cannot use it as an
 * interface either so it has to be cast to the implementation unsafely.
 */
export class ImperfectStub<T> {
    masked(): T {
        return this as unknown as T;
    }
}
