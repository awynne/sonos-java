package org.tensin.sonos.commands;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tensin.sonos.ISonos;
import org.tensin.sonos.commander.SonosCommander;
import org.tensin.sonos.upnp.SonosException;

/**
 * The Class CommandExecutor.
 */
public class ZoneCommandExecutor extends Thread {

    /** The Constant LOGGER. */
    private static final Log LOGGER = LogFactory.getLog(SonosCommander.class);

    /** The sonos zone locker. */
    private final Object sonosZoneLocker = new Object();

    /** The sonos zone. */
    private ISonos sonosZone;

    /** The zone name. */
    private final String zoneName;

    /** The commands queue. */
    private final LinkedBlockingQueue<IZoneCommand> commandsQueue = new LinkedBlockingQueue<IZoneCommand>();

    /** The active. */
    private boolean active = true;

    /** The running command. */
    private boolean runningCommand = false;

    /** The executed commands count. */
    private int executedCommandsCount = 0;

    /**
     * Instantiates a new zone command executor.
     * 
     * @param zoneName
     *            the zone name
     */
    public ZoneCommandExecutor(final String zoneName) {
        super();
        this.zoneName = zoneName;
        setName("SONOS-THREAD-" + zoneName);
    }

    /**
     * Adds the command.
     * 
     * @param command
     *            the command
     */
    public void addCommand(final IZoneCommand command) {
        commandsQueue.offer(command);
    }

    /**
     * Execute command.
     * 
     * @param command
     *            the command
     */
    private void executeCommand(final IZoneCommand command) {
        try {
            LOGGER.info("Executing command [" + command.getName() + "] on zone [" + zoneName + "]");
            command.execute(sonosZone);
            executedCommandsCount++;
        } catch (SonosException e) {
            LOGGER.error("Error while executing command [" + command.getName() + "] on zone [" + zoneName + "]");
        }
    }

    /**
     * Gets the command left count.
     * 
     * @return the command left count
     */
    public int getCommandLeftCount() {
        return commandsQueue.size();
    }

    /**
     * Gets the executed commands count.
     * 
     * @return the executed commands count
     */
    public int getExecutedCommandsCount() {
        return executedCommandsCount;
    }

    /**
     * Gets the sonos zone.
     * 
     * @return the sonos zone
     */
    public ISonos getSonosZone() {
        return sonosZone;
    }

    /**
     * Gets the zone name.
     * 
     * @return the zone name
     */
    public String getZoneName() {
        return zoneName;
    }

    /**
     * Halt.
     */
    public void halt() {
        active = false;
    }

    /**
     * Checks for no command left.
     * 
     * @return true, if successful
     */
    public boolean hasNoCommandLeft() {
        return commandsQueue.size() == 0;
    }

    /**
     * Checks for no running command.
     * 
     * @return true, if successful
     */
    public boolean hasRunningCommand() {
        return runningCommand;
    }

    /**
     * Checks if is sonos zone available.
     * 
     * @return true, if is sonos zone available
     */
    private boolean isSonosZoneAvailable() {
        synchronized (sonosZoneLocker) {
            return sonosZone != null;
        }
    }

    /**
     * Register zone as available.
     * 
     * @param sonos
     *            the sonos
     */
    public void registerZoneAsAvailable(final ISonos sonos) {
        synchronized (sonosZoneLocker) {
            sonosZone = sonos;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        while (active) {
            try {
                if (isSonosZoneAvailable()) {
                    final IZoneCommand command = commandsQueue.take();
                    if (command != null) {
                        if (command instanceof CommandPoisonPill) {
                            active = false;
                        } else {
                            runningCommand = true;
                            executeCommand(command);
                            runningCommand = false;
                        }
                    }
                } else {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                LOGGER.error(e);
            }
        }
    }
}