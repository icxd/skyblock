package net.icxd.dungeons.item.requirement;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class Requirements {
    private final List<Requirement> requirements;
    public Requirements(Requirement... requirements) {
        this.requirements = Arrays.asList(requirements);
    }
}
