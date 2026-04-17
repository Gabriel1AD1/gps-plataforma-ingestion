import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeCheck {
    public static void main(String[] args) {
        long ts = 1776366776000L;
        Instant instant = Instant.ofEpochMilli(ts);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));
        System.out.println("TS: " + formatter.format(instant));
        
        long now = Instant.now().toEpochMilli();
        System.out.println("NOW: " + now);
        System.out.println("NOW ISO: " + formatter.format(Instant.ofEpochMilli(now)));
    }
}
