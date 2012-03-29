package org.tensin.sonos.commands;

import org.tensin.sonos.upnp.SonosException;

/**
 * The Interface ICommand.
 */
public interface IStandardCommand extends ICommand {

	/**
	 * Execute.
	 *
	 * @throws SonosException the sonos exception
	 */
	public void execute() throws SonosException;

}