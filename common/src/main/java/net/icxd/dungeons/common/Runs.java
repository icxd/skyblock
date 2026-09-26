package net.icxd.dungeons.common;

/**
 * The {@code runs} collection: one document per dungeon run, written by the proxy when it sends a
 * party to a dungeon server and updated by that server as the run goes.
 *
 * <pre>
 * _id      run id (a UUID string)
 * server   the dungeon server it's on
 * floor    a {@link DungeonFloor} name
 * leader   UUID string
 * members  UUID strings, the leader first
 * state    assigned (sent, not set up yet), running, or ended
 * created  Date
 * </pre>
 */
public final class Runs {
    public static final String COLLECTION = "runs";

    public static final String SERVER = "server";
    public static final String FLOOR = "floor";
    public static final String LEADER = "leader";
    public static final String MEMBERS = "members";
    public static final String STATE = "state";
    public static final String CREATED = "created";

    public static final String ASSIGNED = "assigned";
    public static final String RUNNING = "running";
    public static final String ENDED = "ended";

    /** A dungeon server whose heartbeat hasn't moved for this long has stopped (as in the Paper plugin's UserStore). */
    public static final long SERVER_STOPPED_AFTER_MILLIS = 12_000;

    private Runs() {
    }
}
