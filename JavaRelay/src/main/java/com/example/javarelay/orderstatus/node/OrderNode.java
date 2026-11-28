package com.example.javarelay.orderstatus.node;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum OrderNode {

    CREATE,
    DISPATCH,
    ROUTE_PRICE,
    RECEIVE,
    ARRIVE,
    FINISH,
    SETTLE,
    FROZEN;

    public Set<OrderNode> directPre() {
        return switch (this) {
            case CREATE, FROZEN -> Set.of();
            case DISPATCH -> Set.of(CREATE);
            case ROUTE_PRICE -> Set.of(CREATE);
            case RECEIVE -> Set.of(DISPATCH);
            case ARRIVE -> Set.of(RECEIVE);
            case FINISH -> Set.of(ARRIVE);
            case SETTLE -> Set.of(FINISH, ROUTE_PRICE);
        };
    }

    public Set<OrderNode> successors() {
        return Arrays.stream(values())
                .filter(n -> n.directPre().contains(this))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<OrderNode> descendants() {
        Set<OrderNode> acc = new LinkedHashSet<>();
        collect(this, acc);
        return acc;
    }

    private static void collect(OrderNode n, Set<OrderNode> acc) {
        for (OrderNode s : n.successors()) {
            if (acc.add(s)) {
                collect(s, acc);
            }
        }
    }
}