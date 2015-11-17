/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.openenergymonitor.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import org.openhab.binding.openenergymonitor.internal.OpenEnergyMonitorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector for serial port communication.
 * 
 * @author Pauli Anttila
 * @since 1.4.0
 */
public class OpenEnergyMonitorSerialConnector extends
		OpenEnergyMonitorConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(OpenEnergyMonitorSerialConnector.class);
	
	int baudRate;
	String portName = null;
	SerialPort serialPort = null;
	InputStream in = null;

	public OpenEnergyMonitorSerialConnector(String portName, int baudRate) {
		this.baudRate = baudRate;
		this.portName = portName;
	}

	@Override
	public void connect() throws OpenEnergyMonitorException {

		try {
			CommPortIdentifier portIdentifier = CommPortIdentifier
					.getPortIdentifier(portName);
			if (portIdentifier.isCurrentlyOwned()){
	            logger.warn("OpenEnergyMonitor Error: Port is currently in use");
	        } else {
				CommPort commPort = portIdentifier.open(this.getClass().getName(),
						2000);
				serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
				
				in = serialPort.getInputStream();
				logger.debug("Open Energy Monitor Serial Port message listener started");
	        }
		} catch (Exception e) {
			throw new OpenEnergyMonitorException(e);
		}
	}

	@Override
	public void disconnect() throws OpenEnergyMonitorException {
		logger.debug("Disconnecting");
		
		try {
			logger.debug("Close serial streams");

			if (in != null) {
				in.close();
			}

			in = null;

			if (serialPort != null) {
				logger.debug("Close serial port");
				serialPort.close();
				serialPort = null;
			}

		} catch (IOException e) {
			throw new OpenEnergyMonitorException(e);
		}
		
		logger.debug("Closed");
	}

	@Override
	public byte[] receiveDatagram() throws OpenEnergyMonitorException {
		byte[] buffer = new byte[16384];
		byte buf = '\0';
		
		if (in == null) 
			connect();
		
		try {
			for(int i = 0; buf != -1 && (char)buf != '\n'; i++){
				buf = (byte)this.in.read();
				buffer[i] = buf;
			}
		} catch (IOException e) {
			throw new OpenEnergyMonitorException(
					"Error occured while receiving data", e);
		}
		String msgStr = new String(buffer);
		String[] msg = msgStr.split(" ");
		logger.debug("Received message {}", msgStr);
		
		//Need to discard first token ("OK") and last two "(-79)" or similar and "\r\n"
		byte[] packet = new byte[msg.length-3];
		for(int i = 1; i < msg.length-2; i++){
				packet[i-1] = (byte)Integer.parseInt(msg[i]);				
		}

		return packet;
	}
}
