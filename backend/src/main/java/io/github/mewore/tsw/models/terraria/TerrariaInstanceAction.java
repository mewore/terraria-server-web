package io.github.mewore.tsw.models.terraria;

/**
 * Actions that are assigned to an instance so that the host owning the instance can manipulate it whenever it has an
 * opportunity to do so. Only up to one action can be assigned to an instance at any moment, and the actions are
 * possible to execute only for instances in specific states (e.g. an instance that is still being set up cannot be
 * booted up).
 */
public enum TerrariaInstanceAction {

    /**
     * <b>{@link TerrariaInstanceState#DEFINED}</b> -> <b>{@link TerrariaInstanceState#VALID}</b> ->
     * <b>{@link TerrariaInstanceState#IDLE}</b>
     */
    SET_UP,

    /**
     * <b>{@link TerrariaInstanceState#IDLE}</b> -> <b>{@link TerrariaInstanceState#BOOTING_UP}</b> ->
     * <b>{@link TerrariaInstanceState#WORLD_MENU}</b>
     */
    BOOT_UP,

    /**
     * <b>{@link TerrariaInstanceState#WORLD_MENU}</b> -> <b>{@link TerrariaInstanceState#MOD_MENU}</b>
     */
    GO_TO_MOD_MENU,

    /**
     * <b>{@link TerrariaInstanceState#MOD_MENU}</b> -> <b>{@link TerrariaInstanceState#WORLD_MENU}</b>
     */
    SET_LOADED_MODS,

    /**
     * <b>{@link TerrariaInstanceState#WORLD_MENU}</b> -> <b>{@link TerrariaInstanceState#RUNNING}</b>
     */
    RUN_SERVER,

    /**
     * <b>Any active state</b> -> <b>{@link TerrariaInstanceState#IDLE}</b> (if it's running, the world is saved)
     */
    SHUT_DOWN,

    /**
     * <b>Any active state</b> -> <b>{@link TerrariaInstanceState#IDLE}</b> (if it's running, the world is NOT saved)
     */
    SHUT_DOWN_NO_SAVE,

    /**
     * <b>Any active state</b> -> <b>{@link TerrariaInstanceState#IDLE}</b> (the instance is killed forcefully)
     */
    TERMINATE,

    /**
     * <b>Any inactive state</b> -> <b>DELETED</b>
     */
    DELETE;

    public boolean isApplicableTo(final TerrariaInstanceState state) {
        return state.isActionApplicable(this);
    }
}
