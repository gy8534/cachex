import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Cachex<K, V> {
    private final int capacity;
    private final Duration defaultTTL;

    private final Map<K, Node<K, V>> map;
    private final PriorityQueue<Node<K, V>> expiryOrder;
    private final  Node<K, V> head;
    private final  Node<K, V> tail;

    public Cachex(int capacity) {
        this(capacity, Duration.ofMinutes(10));
    }

    public Cachex(int capacity, Duration ttl) {
        if (capacity <= 0) {
            throw  new IllegalArgumentException("Capacity must be greater than 0");
        }

        if (isInvalidTTL(ttl)) {
            throw new IllegalArgumentException("Valid TTL is required");
        }

        this.defaultTTL = ttl;
        this.capacity = capacity;
        map = new HashMap<>();
        expiryOrder = new PriorityQueue<>(Comparator.comparingLong(node -> node.expiresAt));
        head = new Node<>(null, null, 0);
        tail = new Node<>(null, null, 0);
        head.next = tail;
        tail.prev = head;
    }

    public void put(K key, V val) {
        put(key, val, defaultTTL);
    }

    public void put(K key, V val, Duration ttl) {
        if (isInvalidTTL(ttl)) {
            throw new IllegalArgumentException("Valid TTL is required");
        }

        evictExpired();

        Node<K, V> node = map.get(key);
        long expiresAt = now() + ttl.toMillis();

        if (node != null) {
            node.value = val;
            expiryOrder.remove(node);
            node.expiresAt = expiresAt;
            expiryOrder.offer(node);
            moveToFront(node);
            return;
        }

        Node<K, V> newNode = new Node<>(key, val, expiresAt);
        map.put(key, newNode);
        expiryOrder.offer(newNode);
        addFirst(newNode);

        if (map.size() > capacity) {
            removeLast();
        }
    }

    public V get(K key) {
        Node<K, V> node = map.get(key);

        if (node == null) return null;

        if (isExpired(node)) {
            remove(key);
            return null;
        }

        moveToFront(node);

        return node.value;
    }

    public void remove(K key) {
        Node<K, V> node = map.remove(key);

        if (node != null) {
            expiryOrder.remove(node);
            removeNode(node);
        }
    }

    public int size() {
        return this.map.size();
    }

    public boolean containsKey(K key) {
        Node<K, V> node = map.get(key);

        if (node == null) return false;
        if (isExpired(node)) {
            remove(key);
            return false;
        }

        return true;
    }

    public void clear() {
        map.clear();
        expiryOrder.clear();
        head.next = tail;
        tail.prev = head;
    }

    private void removeNode(Node<K, V> node) {
        if (node == head || node == tail) {
            return;
        }

        Node<K, V> prev = node.prev;
        Node<K, V> next = node.next;

        prev.next = next;
        next.prev = prev;

        node.prev = null;
        node.next = null;
    }

    private void addFirst(Node<K, V> node) {
        Node<K, V> next = head.next;

        head.next = node;
        node.prev = head;

        node.next = next;
        next.prev = node;
    }

    private void removeLast() {
        Node<K, V> last = tail.prev;

        if (last == head) return;

        map.remove(last.key);
        expiryOrder.remove(last);
        removeNode(last);
    }

    private void moveToFront(Node<K, V> node) {
        removeNode(node);
        addFirst(node);
    }

    private void evictExpired() {
        while(!expiryOrder.isEmpty() && expiryOrder.peek().expiresAt <= now()) {
            Node<K, V> node = expiryOrder.poll();

            if (node != null) {
                map.remove(node.key);
                removeNode(node);
            }
        }
    }

    private static class Node<K, V> {
        private final K key;
        private V value;
        private long expiresAt;

        private Node<K, V> prev;
        private Node<K, V> next;

        public Node(K key, V value, long expiresAt) {
            this.key = key;
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    private boolean isInvalidTTL(Duration ttl) {
        return ttl == null || ttl.isZero() || ttl.isNegative();
    }

    private boolean isExpired(Node<K, V> node) {
        return now() > node.expiresAt;
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
