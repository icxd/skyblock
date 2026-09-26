package net.icxd.dungeons;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A listener that's only registered on these server types (and on {@link SkyBlockServer.Type#NONE},
 * which runs everything). Without it, a listener runs everywhere.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OnlyOn {
    SkyBlockServer.Type[] value();
}
