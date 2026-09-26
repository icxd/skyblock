package net.icxd.dungeons.command;

import net.icxd.dungeons.user.Rank;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value= RetentionPolicy.RUNTIME)
public @interface CommandParameters {
    public String description() default "";

    public String usage() default "/<command>";

    public String aliases() default "";

    public Rank permission() default Rank.DEFAULT;
}

