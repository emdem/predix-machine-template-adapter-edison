/*
 * Copyright (c) 2014 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predix.solsvc.workshop.types;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import com.ge.dspmicro.machinegateway.types.PDataNode;

import mraa.Aio;
import mraa.Gpio;

/**
 * 
 * 
 * @author Predix Machine Sample
 */
public class WorskshopDataNodeIntel extends PDataNode
{
    
    /**
	 * @param machineAdapterId -
	 * @param name -
     * @param nodeType - 
     * @param nodePin -
	 */
	public WorskshopDataNodeIntel(UUID machineAdapterId, String name,String nodeType,long nodePin) {
		super(machineAdapterId, name);
		this.nodeType = nodeType;
		this.nodePin = nodePin;
		switch (this.nodeType) {
        case "Light": //$NON-NLS-1$
			this.lightNode = new upm_grove.GroveLight(nodePin);
			break;
        case "Temperature": //$NON-NLS-1$
        	this.tempNode = new upm_grove.GroveTemp(nodePin);
        	break;
        case "RotaryAngle": //$NON-NLS-1$
        	this.rotaryNode = new upm_grove.GroveRotary(nodePin);
        	break;
        case "Button": //$NON-NLS-1$
        	this.setButtonNode(new upm_grove.GroveButton(nodePin));
        	break;
		default:
			break;
        }
	}

	private long nodePin;
        
    private Gpio gpioPin;
    
    private Aio aioPin;
    
    private upm_grove.GroveLight lightNode;
    
    private upm_grove.GroveTemp tempNode;
    
    private upm_grove.GroveRotary rotaryNode;
    
    private upm_grove.GroveButton buttonNode;

    private String nodeType;

	
	
    

    /**
     * Node address to uniquely identify the node.
     */
    @Override
    public URI getAddress()
    {
        try
        {
            URI address = new URI("sample.subscription.adapter", null, "localhost", -1, "/" + getName(), null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return address;
        }
        catch (URISyntaxException e)
        {
            return null;
        }
    }

	/**
	 * @return -
	 */
	public long getNodePin() {
		return this.nodePin;
	}

	/**
	 * @param nodePin -
	 */
	public void setNodePin(long nodePin) {
		this.nodePin = nodePin;
	}

	/**
	 * @return -
	 */
	public Aio getAioPin() {
		return this.aioPin;
	}

	/**
	 * @param aioPin -
	 */
	public void setAioPin(Aio aioPin) {
		this.aioPin = aioPin;
	}

	/**
	 * @return -
	 */
	public Gpio getGpioPin() {
		return this.gpioPin;
	}

	/**
	 * @param gpioPin -
	 */
	public void setGpioPin(Gpio gpioPin) {
		this.gpioPin = gpioPin;
	}

	/**
	 * @return -
	 */
	public upm_grove.GroveLight getLightNode() {
		return this.lightNode;
	}

	/**
	 * @param lightNode -
	 */
	public void setLightNode(upm_grove.GroveLight lightNode) {
		this.lightNode = lightNode;
	}

	/**
	 * @return -
	 */
	public upm_grove.GroveTemp getTempNode() {
		return this.tempNode;
	}

	/**
	 * @param tempNode -
	 */
	public void setTempNode(upm_grove.GroveTemp tempNode) {
		this.tempNode = tempNode;
	}

	/**
	 * @return -
	 */
	public upm_grove.GroveRotary getRotaryNode() {
		return this.rotaryNode;
	}

	/**
	 * @param rotaryNode -
	 */
	public void setRotaryNode(upm_grove.GroveRotary rotaryNode) {
		this.rotaryNode = rotaryNode;
	}

	/**
	 * @return -
	 */
	public String getNodeType() {
		return this.nodeType;
	}

	/**
	 * @param nodeType -
	 */
	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	/**
	 * @return -
	 */
	public upm_grove.GroveButton getButtonNode() {
		return this.buttonNode;
	}

	/**
	 * @param buttonNode -
	 */
	public void setButtonNode(upm_grove.GroveButton buttonNode) {
		this.buttonNode = buttonNode;
	}	
}
