package com.seedxray.world.gen;

import java.util.concurrent.Semaphore;

public interface SemaphoreHolder {
    Semaphore getMutex();
}
