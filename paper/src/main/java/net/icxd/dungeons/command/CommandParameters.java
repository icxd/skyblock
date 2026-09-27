package net.icxd.dungeons.command;

import net.icxd.dungeons.common.Rank;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value= RetentionPolicy.RUNTIME)
public @interface CommandParameters {
    public String description() default "";

    public String usage() default "/<command>";

    public String aliases() default "";

    public Rank permission() default Rank.DEFAULT;

    /**
     * A Sandbox tool: free for anyone on a Sandbox profile, else it needs {@link #permission}. For
     * commands that only give items or change the one held.
     */
    public boolean sandbox() default false;
}

