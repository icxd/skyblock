package net.icxd.dungeons.utils;

/**
 * SkyBlock's calendar: a day lasts 20 real minutes, a month 31 days and a year 12 months, counted
 * from Year 1, Early Spring 1st at 17:55 UTC on 11 June 2019. Checked against recordings of Hypixel's
 * sidebar ("Early Autumn 8th", "10:30am" and "5:20pm" at the times they were taken).
 */
public record SkyBlockTime(int year, int month, int day, int hour, int minute) {
    public static final long EPOCH = 1_560_275_700_000L;
    public static final long DAY_MS = 20 * 60 * 1000;
    public static final long MONTH_MS = 31 * DAY_MS;
    public static final long YEAR_MS = 12 * MONTH_MS;

    private static final String[] MONTHS = {"Early Spring", "Spring", "Late Spring", "Early Summer", "Summer", "Late Summer",
            "Early Autumn", "Autumn", "Late Autumn", "Early Winter", "Winter", "Late Winter"};

    public static SkyBlockTime now() {
        return at(System.currentTimeMillis());
    }

    public static SkyBlockTime at(long millis) {
        long elapsed = millis - EPOCH;
        long minutes = (elapsed % DAY_MS) * 24 * 60 / DAY_MS;
        return new SkyBlockTime((int) (elapsed / YEAR_MS) + 1, (int) (elapsed % YEAR_MS / MONTH_MS), (int) (elapsed % MONTH_MS / DAY_MS) + 1,
                (int) (minutes / 60), (int) (minutes % 60));
    }

    /** "Early Autumn 8th". */
    public String date() {
        return MONTHS[month] + " " + day + ordinal(day);
    }

    /** "10:30am": to the ten minutes, as the sidebar shows it. */
    public String clock() {
        int twelve = hour % 12 == 0 ? 12 : hour % 12;
        return twelve + ":" + (minute / 10) + "0" + (hour < 12 ? "am" : "pm");
    }

    /** Daytime is 6:00am to 7:00pm; the sidebar shows a sun then, a moon otherwise. */
    public boolean isDay() {
        return hour >= 6 && hour < 19;
    }

    private static String ordinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) return "th";
        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
