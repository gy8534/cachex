import java.time.Duration;

public class Main {
    public static void main(String[] args) {
        Cachex<Integer, Integer> cache = new Cachex<>(3, Duration.ofMinutes(1));

        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        cache.put(4, 4);

        System.out.println("Is key exits - " + cache.containsKey(1));
    }
}