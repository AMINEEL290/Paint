package com.paint.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistoryServiceTest {
    @Test
    void supportsUndoRedoAndDropsRedoBranchAfterNewAction() {
        HistoryService<String> history = new HistoryService<>(5);
        history.reset("blank");
        history.record("stroke");
        history.record("shape");

        assertEquals("stroke", history.undo().orElseThrow());
        assertEquals("shape", history.redo().orElseThrow());
        assertEquals("stroke", history.undo().orElseThrow());

        history.record("text");
        assertFalse(history.canRedo());
        assertEquals("stroke", history.undo().orElseThrow());
    }

    @Test
    void keepsHistoryWithinConfiguredLimit() {
        HistoryService<Integer> history = new HistoryService<>(3);
        history.reset(0);
        history.record(1);
        history.record(2);
        history.record(3);

        assertEquals(3, history.size());
        assertEquals(2, history.undo().orElseThrow());
        assertEquals(1, history.undo().orElseThrow());
        assertFalse(history.canUndo());
    }
}
