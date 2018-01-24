/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ring.handler;

import static org.openhab.binding.ring.RingBindingConstants.*;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.ring.internal.RingDeviceRegistry;
import org.openhab.binding.ring.internal.data.RingDevice;
import org.openhab.binding.ring.internal.errors.DeviceNotFoundException;
import org.openhab.binding.ring.internal.errors.IllegalDeviceClassException;

/**
 * The {@link RingDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Wim Vissers - Initial contribution
 */
public abstract class RingDeviceHandler extends AbstractRingHandler {

    /**
     * The RingDevice instance linked to this thing.
     */
    protected RingDevice device;

    public RingDeviceHandler(Thing thing) {
        super(thing);
    }

    /**
     * Link the device, and update the device with the status CONFIGURED.
     *
     * @param id the device id
     * @param deviceClass the expected class
     * @throws DeviceNotFoundException when device is not found in the RingDeviceRegistry.
     * @throws IllegalDeviceClassException when the regitered device is of the wrong type.
     */
    protected void linkDevice(String id, Class<?> deviceClass)
            throws DeviceNotFoundException, IllegalDeviceClassException {
        device = RingDeviceRegistry.getInstance().getRingDevice(id);
        if (device.getClass().equals(deviceClass)) {
            device.setRegistrationStatus(RingDeviceRegistry.Status.CONFIGURED);
            device.setRingDeviceHandler(this);
        } else {
            throw new IllegalDeviceClassException(
                    "Class '" + deviceClass.getName() + "' expected but '" + device.getClass().getName() + "' found.");
        }
    }

    /**
     * Handle generic commands, common to all Ring Devices.
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof Number || command instanceof RefreshType || command instanceof IncreaseDecreaseType
                || command instanceof UpDownType) {
            switch (channelUID.getId()) {
                case CHANNEL_CONTROL_STATUS:
                    updateState(channelUID, status);
                    break;
                case CHANNEL_CONTROL_ENABLED:
                    updateState(channelUID, enabled);
                    break;
                default:
                    logger.debug("Command received for an unknown channel: {}", channelUID.getId());
                    break;
            }
            refreshState();
        } else if (command instanceof OnOffType) {
            OnOffType xcommand = (OnOffType) command;
            switch (channelUID.getId()) {
                case CHANNEL_CONTROL_STATUS:
                    status = xcommand;
                    updateState(channelUID, status);
                    break;
                case CHANNEL_CONTROL_ENABLED:
                    if (!enabled.equals(xcommand)) {
                        enabled = xcommand;
                        updateState(channelUID, enabled);
                        if (enabled.equals(OnOffType.ON)) {
                            startAutomaticRefresh();
                        } else {
                            stopAutomaticRefresh();
                        }
                    }
                    break;
                default:
                    logger.debug("Command received for an unknown channel: {}", channelUID.getId());
                    break;
            }
        } else {
            logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        }
    }

}
