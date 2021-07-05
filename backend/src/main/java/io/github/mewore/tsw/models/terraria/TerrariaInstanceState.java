package io.github.mewore.tsw.models.terraria;

/**
 * An automaton-like state of a terraria instance.
 * <p>
 * Expected transitions of states:
 * <ul>
 *     <li><b>{@code non-existent}</b> -[created by any host]-> <b>{@code DEFINED}</b></li>
 *     <li><b>{@code any state}</b> -[unrecoverable error encountered]-> <b>{@code INVALID}</b></li>
 *     <li>(TODO) <b>{@code BROKEN}</b> -[clean up]-> <b>{@code DEFINED}</b></li>
 *     <li><b>{@code DEFINED}</b> -[detected by owning host]-> <b>{@code VALID}</b></li>
 *     <li><b>{@code VALID}</b> -[finished downloading]-> <b>{@code DOWNLOADED}</b></li>
 *     <li><b>{@code DOWNLOADED}</b> -[finished unpacking]-> <b>{@code READY}</b></li>
 * </ul>
 */
public enum TerrariaInstanceState {

    DEFINED, VALID, READY, INVALID, BROKEN
}
