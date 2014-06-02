package com.scottbezek.embarcadero.app.statestream;

public interface ListenerChangeListener {
    void onFirstListenerRegistered();
    void onLastListenerUnregistered();
}
