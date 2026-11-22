package com.paint.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HistoryService<T> {
    private int limit;
    private final List<T> states = new ArrayList<>();
    private int currentIndex = -1;

    public HistoryService(int limit) {
        if (limit < 2) {
            throw new IllegalArgumentException("History limit must be at least 2");
        }
        this.limit = limit;
    }

    public void reset(T initialState) {
        states.clear();
        states.add(initialState);
        currentIndex = 0;
    }

    public void setLimit(int limit) {
        if (limit < 2) {
            throw new IllegalArgumentException("History limit must be at least 2");
        }
        this.limit = limit;
        while (states.size() > limit) {
            states.remove(0);
            currentIndex--;
        }
        currentIndex = Math.max(0, currentIndex);
    }

    public void record(T state) {
        if (currentIndex < 0) {
            reset(state);
            return;
        }
        while (states.size() > currentIndex + 1) {
            states.remove(states.size() - 1);
        }
        states.add(state);
        if (states.size() > limit) {
            states.remove(0);
        } else {
            currentIndex++;
        }
        currentIndex = states.size() - 1;
    }

    public Optional<T> undo() {
        if (!canUndo()) {
            return Optional.empty();
        }
        return Optional.of(states.get(--currentIndex));
    }

    public Optional<T> redo() {
        if (!canRedo()) {
            return Optional.empty();
        }
        return Optional.of(states.get(++currentIndex));
    }

    public boolean canUndo() {
        return currentIndex > 0;
    }

    public boolean canRedo() {
        return currentIndex >= 0 && currentIndex < states.size() - 1;
    }

    public int size() {
        return states.size();
    }
}
