package net.icxd.dungeons.anticheat.check;

import lombok.Getter;

@Getter
public class CheckResult {
    private final Check check;
    private final boolean passed;
    private final String message;

    public CheckResult(Check check, boolean passed, String message) {
        this.check = check;
        this.passed = passed;
        this.message = message;
    }

    public CheckResult(Check check, boolean passed) {
        this(check, passed, null);
    }
}
