package com.example.myokdownload.dowload.core;

public abstract class NamedRunnable implements Runnable{
    protected final String name;

    public NamedRunnable(String name) {
        this.name = name;
    }

    @Override
    public final void run() {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(name);
        try {
            execute();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            interrupted(e);
        } finally {
            Thread.currentThread().setName(oldName);
            finished();
        }
    }

    protected abstract void execute() throws InterruptedException;
    protected abstract void interrupted(InterruptedException e);
    protected abstract void finished();
}
