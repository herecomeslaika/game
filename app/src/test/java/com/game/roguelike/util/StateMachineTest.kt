package com.game.roguelike.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class StateMachineTest {

    enum class TestState { IDLE, RUNNING, JUMPING }

    @Test
    fun `initial state is set correctly`() {
        val sm = StateMachine(TestState.IDLE)
        assertEquals(TestState.IDLE, sm.currentState)
    }

    @Test
    fun `transition changes current and previous state`() {
        val sm = StateMachine(TestState.IDLE)
        sm.transitionTo(TestState.RUNNING)
        assertEquals(TestState.RUNNING, sm.currentState)
        assertEquals(TestState.IDLE, sm.previousState)
    }

    @Test
    fun `transition to same state does nothing`() {
        val sm = StateMachine(TestState.IDLE)
        sm.transitionTo(TestState.IDLE)
        assertEquals(TestState.IDLE, sm.currentState)
    }

    @Test
    fun `stateTime resets on transition`() {
        val sm = StateMachine(TestState.IDLE)
        sm.update(0.5f)
        sm.update(0.5f)
        assertEquals(1f, sm.stateTime, 0.001f)
        sm.transitionTo(TestState.RUNNING)
        assertEquals(0f, sm.stateTime, 0.001f)
    }

    @Test
    fun `stateTime increments on update`() {
        val sm = StateMachine(TestState.IDLE)
        sm.update(0.3f)
        assertEquals(0.3f, sm.stateTime, 0.001f)
        sm.update(0.2f)
        assertEquals(0.5f, sm.stateTime, 0.001f)
    }

    @Test
    fun `onEnter listener fires with previous state`() {
        val sm = StateMachine(TestState.IDLE)
        var enteredFrom: TestState? = null
        sm.onEnter(TestState.RUNNING) { prev -> enteredFrom = prev }
        sm.transitionTo(TestState.RUNNING)
        assertEquals(TestState.IDLE, enteredFrom)
    }

    @Test
    fun `onEnter does not fire on same-state transition`() {
        val sm = StateMachine(TestState.IDLE)
        var fireCount = 0
        sm.onEnter(TestState.IDLE) { fireCount++ }
        sm.transitionTo(TestState.IDLE)
        assertEquals(0, fireCount)
    }

    @Test
    fun `onEnter only fires for its registered state`() {
        val sm = StateMachine(TestState.IDLE)
        var runningFired = false
        var jumpingFired = false
        sm.onEnter(TestState.RUNNING) { runningFired = true }
        sm.onEnter(TestState.JUMPING) { jumpingFired = true }
        sm.transitionTo(TestState.RUNNING)
        assertTrue(runningFired)
        assertFalse(jumpingFired)
    }

    @Test
    fun `multiple transitions track history correctly`() {
        val sm = StateMachine(TestState.IDLE)
        sm.transitionTo(TestState.RUNNING)
        assertEquals(TestState.IDLE, sm.previousState)
        sm.transitionTo(TestState.JUMPING)
        assertEquals(TestState.RUNNING, sm.previousState)
        assertEquals(TestState.JUMPING, sm.currentState)
    }

    @Test
    fun `justEntered returns true right after transition`() {
        val sm = StateMachine(TestState.IDLE)
        assertFalse(sm.justEntered())
        sm.transitionTo(TestState.RUNNING)
        assertTrue(sm.justEntered())
        sm.update(0.01f)
        assertFalse(sm.justEntered())
    }
}