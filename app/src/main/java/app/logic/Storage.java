package app.logic;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Storage {

    public ConcurrentHashMap<String, Integer> item_table =
        new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Integer> price_table =
        new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Set<String>> groups =
        new ConcurrentHashMap<>();
}
