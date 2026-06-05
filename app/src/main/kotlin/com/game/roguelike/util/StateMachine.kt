package com.game.roguelike.util

class StateMachine<T : Enum<T>>(initialState: T) {
    var currentState: T = initialState
        private set
    var previousState: T = initialState
        private set
    var stateTime: Float = 0f
        private set

    private val listeners = mutableMapOf<T, MutableList<(T) -> Unit>>()
    private var enteredThisState = false

    fun transitionTo(newState: T) {
        if (newState == currentState) return
        previousState = currentState
        currentState = newState
        stateTime = 0f
        enteredThisState = true
        listeners[newState]?.forEach { it(previousState) }
    }

    fun update(dt: Float) {
        stateTime += dt
        enteredThisState = false
    }

    fun onEnter(state: T, listener: (fromState: T) -> Unit) {
        listeners.getOrPut(state) { mutableListOf() }.add(listener)
    }

    fun justEntered(): Boolean = enteredThisState
}
