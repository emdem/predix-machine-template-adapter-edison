/*
 * Copyright (c) 2014 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predix.solsvc.workshop.adapter;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ge.dspmicro.machinegateway.api.adapter.IDataSubscription;
import com.ge.dspmicro.machinegateway.api.adapter.IDataSubscriptionListener;
import com.ge.dspmicro.machinegateway.api.adapter.IMachineAdapter;
import com.ge.dspmicro.machinegateway.api.adapter.ISubscriptionAdapterListener;
import com.ge.dspmicro.machinegateway.api.adapter.ISubscriptionMachineAdapter;
import com.ge.dspmicro.machinegateway.api.adapter.MachineAdapterException;
import com.ge.dspmicro.machinegateway.api.adapter.MachineAdapterInfo;
import com.ge.dspmicro.machinegateway.api.adapter.MachineAdapterState;
import com.ge.dspmicro.machinegateway.types.PDataNode;
import com.ge.dspmicro.machinegateway.types.PDataValue;
import com.ge.dspmicro.machinegateway.types.PEnvelope;
import com.ge.predix.solsvc.workshop.config.JsonDataNode;
import com.ge.predix.solsvc.workshop.types.WorkshopDataSubscription;
import com.ge.predix.solsvc.workshop.types.WorkshopSubscriptionListener;
import com.ge.predix.solsvc.workshop.types.WorskshopDataNodeIntel;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;
import upm_grove.GroveLed;

/**
 * 
 * @author Predix Machine Sample
 */
@Component(name = IntelBoardSubscriptionMachineAdapter.SERVICE_PID, provide =
{
        ISubscriptionMachineAdapter.class, IMachineAdapter.class
}, designate = IntelBoardSubscriptionMachineAdapter.Config.class, configurationPolicy = ConfigurationPolicy.require)
public class IntelBoardSubscriptionMachineAdapter
        implements ISubscriptionMachineAdapter
{
    // Meta mapping for configuration properties
    /**
     * 
     * @author 212546387 -
     */
    @Meta.OCD(name = "%component.name", localization = "OSGI-INF/l10n/bundle")
    interface Config
    {
        /**
         * @return -
         */
        @Meta.AD(name = "%updateInterval.name", description = "%updateInterval.description", id = UPDATE_INTERVAL, required = false, deflt = "")
        String updateInterval();

        /**
         * @return -
         */
        @Meta.AD(name = "%nodeConfigFile.name", description = "%nodeConfigFile.description", id = NODE_NAMES, required = false, deflt = "")
        String nodeConfigFile();

        /**
         * @return -
         */
        @Meta.AD(name = "%adapterName.name", description = "%adapterName.description", id = ADAPTER_NAME, required = false, deflt = "")
        String adapterName();

        /**
         * @return -
         */
        @Meta.AD(name = "%adapterDescription.name", description = "%adapterDescription.description", id = ADAPTER_DESCRIPTION, required = false, deflt = "")
        String adapterDescription();

        /**
         * @return -
         */
        @Meta.AD(id = DATA_SUBSCRIPTIONS, name = "%dataSubscriptions.name", description = "%dataSubscriptions.description", required = true, deflt = "")
        String dataSubscriptions();
    }

    /** Service PID for Sample Machine Adapter */
    public static final String                SERVICE_PID         = "com.ge.predix.solsvc.workshop.adapter";         //$NON-NLS-1$
    /** Key for Node Configuration File*/
    public static final String 				  NODE_CONFI_FILE 	  = SERVICE_PID+"configFile";                                    //$NON-NLS-1$
    /** Key for Update Interval */
    public static final String                UPDATE_INTERVAL     = SERVICE_PID + ".UpdateInterval";                             //$NON-NLS-1$
    /** Key for number of nodes */
    public static final String                NODE_NAMES          = SERVICE_PID + ".NodeConfigFile";                              //$NON-NLS-1$
    /** key for machine adapter name */
    public static final String                ADAPTER_NAME        = SERVICE_PID + ".Name";                                       //$NON-NLS-1$
    /** Key for machine adapter description */
    public static final String                ADAPTER_DESCRIPTION = SERVICE_PID + ".Description";                                //$NON-NLS-1$
    /** data subscriptions */
    public static final String                DATA_SUBSCRIPTIONS  = SERVICE_PID + ".DataSubscriptions";                          //$NON-NLS-1$
    /** The regular expression used to split property values into String array. */
    public final static String                SPLIT_PATTERN       = "\\s*\\|\\s*";                                               //$NON-NLS-1$

    /**
     * 
     */
    public final static String 				  MACHINE_HOME		  = System.getProperty("predix.home.dir"); 					 	 //$NON-NLS-1$
    // Create logger to report errors, warning massages, and info messages (runtime Statistics)
    private static final Logger               _logger             = LoggerFactory
                                                                          .getLogger(IntelBoardSubscriptionMachineAdapter.class);
    private UUID                              uuid                = UUID.randomUUID();
    private Dictionary<String, Object>        props;
    private MachineAdapterInfo                adapterInfo;
    private MachineAdapterState               adapterState;
    private Map<UUID, WorskshopDataNodeIntel>         dataNodes           = new HashMap<UUID, WorskshopDataNodeIntel>();

    private int                               updateInterval;

    private Config                            config;

    /**
     * Data cache for holding latest data updates
     */
    protected Map<UUID, PDataValue>           dataValueCache      = new ConcurrentHashMap<UUID, PDataValue>();
    private Map<UUID, WorkshopDataSubscription> dataSubscriptions   = new HashMap<UUID, WorkshopDataSubscription>();

    private IDataSubscriptionListener         dataUpdateHandler   = new WorkshopSubscriptionListener();

    private static int ADC_REF=5;

	@SuppressWarnings("static-access")
	private final int chord[] = {
			upm_buzzer.javaupm_buzzer.DO, 
			upm_buzzer.javaupm_buzzer.RE,
			upm_buzzer.javaupm_buzzer.MI, 
			upm_buzzer.javaupm_buzzer.FA,
			upm_buzzer.javaupm_buzzer.SOL, 
			upm_buzzer.javaupm_buzzer.LA,
			upm_buzzer.javaupm_buzzer.SI};
    /*
     * ###############################################
     * # OSGi service lifecycle management #
     * ###############################################
     */

    /**
     * OSGi component lifecycle activation method
     * 
     * @param ctx component context
     * @throws IOException on fail to load/set configuration properties
     */
    @Activate
    public void activate(ComponentContext ctx)
            throws IOException
    {
        
        _logger.info("Starting sample " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        try {
        	_logger.info("Library Path : "+System.getProperty("java.library.path")); //$NON-NLS-1$ //$NON-NLS-2$
        	_logger.info("Env : "+System.getenv()); //$NON-NLS-1$
            System.loadLibrary("mraajava"); //$NON-NLS-1$
        } catch (Exception e) {
        	_logger.error(
                    "Native code library failed to load. See the chapter on Dynamic Linking Problems in the SWIG Java documentation for help.\n" + //$NON-NLS-1$
                            e);
        }
        // Get all properties and create nodes.
        this.props = ctx.getProperties();

        this.config = Configurable.createConfigurable(Config.class, ctx.getProperties());

        this.updateInterval = Integer.parseInt(this.config.updateInterval());
        ObjectMapper mapper = new ObjectMapper();
        File configFile = new File(MACHINE_HOME+File.separator+this.config.nodeConfigFile());
        List<JsonDataNode> nodes = mapper.readValue(configFile, new TypeReference<List<JsonDataNode>>()
        {
            //
        });
        createNodes(nodes);

        this.adapterInfo = new MachineAdapterInfo(this.config.adapterName(),
                IntelBoardSubscriptionMachineAdapter.SERVICE_PID, this.config.adapterDescription(), ctx
                        .getBundleContext().getBundle().getVersion().toString());

        List<String> subs = Arrays.asList(parseDataSubscriptions(DATA_SUBSCRIPTIONS));
        // Start data subscription and sign up for data updates.
        for (String sub : subs)
        {
            WorkshopDataSubscription dataSubscription = new WorkshopDataSubscription(this, sub, this.updateInterval,
                    new ArrayList<PDataNode>(this.dataNodes.values()));
            this.dataSubscriptions.put(dataSubscription.getId(), dataSubscription);
            // Using internal listener, but these subscriptions can be used with Spillway listener also
            dataSubscription.addDataSubscriptionListener(this.dataUpdateHandler);
            new Thread(dataSubscription).start();
        }
    }

    private String[] parseDataSubscriptions(String key)
    {
    	
        Object objectValue = this.props.get(key);
        _logger.info("Key : "+key+" : "+objectValue); //$NON-NLS-1$ //$NON-NLS-2$
        if ( objectValue == null )
        {
            invalidDataSubscription();
        }else {

	        if ( objectValue instanceof String[] )
	        {
	            if ( ((String[]) objectValue).length == 0 )
	            {
	                invalidDataSubscription();
	            }
	            return (String[]) objectValue;
	        }
	
	        String stringValue = objectValue.toString();
	        if ( stringValue.length() > 0 )
	        {
	            return stringValue.split(SPLIT_PATTERN);
	        }
        }
        invalidDataSubscription();
        return new String[0];
    }

    
    private void invalidDataSubscription()
    {
        // data subscriptions must not be empty.
        String msg = "SampleSubscriptionAdapter.dataSubscriptions.invalid"; //$NON-NLS-1$
        _logger.error(msg);
        throw new MachineAdapterException(msg);
    }

    /**
     * OSGi component lifecycle deactivation method
     * 
     * @param ctx component context
     */
    @Deactivate
    public void deactivate(ComponentContext ctx)
    {
        // Put your clean up code here when container is shutting down
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Stopped sample for " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }

        Collection<WorkshopDataSubscription> values = this.dataSubscriptions.values();
        // Stop random data generation thread.
        for (WorkshopDataSubscription sub : values)
        {
            sub.stop();
        }
        this.adapterState = MachineAdapterState.Stopped;
    }

    /**
     * OSGi component lifecycle modified method. Called when
     * the component properties are changed.
     * 
     * @param ctx component context
     */
    @Modified
    public synchronized void modified(ComponentContext ctx)
    {
        // Handle run-time changes to properties.

        this.props = ctx.getProperties();
    }

    /*
     * #######################################
     * # IMachineAdapter interface methods #
     * #######################################
     */

    @Override
	public UUID getId()
    {
        return this.uuid;
    }

    @Override
	public MachineAdapterInfo getInfo()
    {
        return this.adapterInfo;
    }

    @Override
	public MachineAdapterState getState()
    {
        return this.adapterState;
    }

    /*
     * Returns all data nodes. Data nodes are auto-generated at startup.
     */
    @Override
	public List<PDataNode> getNodes()
    {
        return new ArrayList<PDataNode>(this.dataNodes.values());
    }

    /*
     * Reads data from data cache. Data cache always contains latest values.
     */
    @Override
	public PDataValue readData(UUID nodeId)
            throws MachineAdapterException
    {
        PDataValue pDataValue = new PDataValue(nodeId);
        float fvalue = 0.0f;
    	WorskshopDataNodeIntel node = this.dataNodes.get(nodeId);
    	switch (node.getNodeType()) {
    		case "Light": //$NON-NLS-1$
    			fvalue = node.getLightNode().value();
    		break;
    		case "Temperature": //$NON-NLS-1$
    			fvalue = node.getTempNode().value();
    			break;
    		case "RotaryAngle": //$NON-NLS-1$
    			float sensorValue = node.getRotaryNode().abs_value();
    			float calculatedValue = Math.round((sensorValue) * ADC_REF / 1023);
    			DecimalFormat df = new DecimalFormat("####.##"); //$NON-NLS-1$
    			fvalue = new Float(df.format(calculatedValue));
    			if (calculatedValue > 3.0) {
    				GroveLed ledPin = new GroveLed(3);
    				ledPin.write(1);
    			}else {
    				GroveLed ledPin = new GroveLed(3);
    				ledPin.write(0);
    			}
    			break;
    		case "Button": //$NON-NLS-1$
    			int value = node.getButtonNode().value();
    			fvalue = new Float(value);
    			if (value == 1) {
    				upm_buzzer.Buzzer sound = new upm_buzzer.Buzzer(5);
    				for (int i = 0; i < this.chord.length; i++) {
    					// play each note for one second
    					sound.playSound(this.chord[i], 1000000);
    				}
    				// ! [Interesting]
    				sound.stopSound();
    			}
    			break;
    		default:
    			fvalue = node.getAioPin().readFloat();
    			break;
    	}
    	
        PEnvelope envelope = new PEnvelope(fvalue);
        pDataValue = new PDataValue(node.getNodeId(), envelope);
        pDataValue.setNodeName(node.getName());
        //pDataValue.setAddress(node.getAddress());
        // Do not return null.
        return pDataValue;
    }

    /*
     * Writes data value into data cache.
     */
    @Override
	public void writeData(UUID nodeId, PDataValue value)
            throws MachineAdapterException
    {
        if ( this.dataValueCache.containsKey(nodeId) )
        {
            // Put data into cache. The value typically should be written to a device node.
            this.dataValueCache.put(nodeId, value);
        }
    }

    /*
     * ###################################################
     * # ISubscriptionMachineAdapter interface methods #
     * ###################################################
     */

    /*
     * Returns list of all subscriptions.
     */
    @Override
	public List<IDataSubscription> getSubscriptions()
    {
        return new ArrayList<IDataSubscription>(this.dataSubscriptions.values());
    }

    /*
     * Adds new data subscription into the list.
     */
    @Override
	public synchronized UUID addDataSubscription(IDataSubscription subscription)
            throws MachineAdapterException
    {
        if ( subscription == null )
        {
            throw new IllegalArgumentException("Subscription is null"); //$NON-NLS-1$
        }

        List<PDataNode> subscriptionNodes = new ArrayList<PDataNode>();

        // Add new data subscription.
        if ( !this.dataSubscriptions.containsKey(subscription.getId()) )
        {
            // Make sure that new subscription contains valid nodes.
            for (PDataNode node : subscription.getSubscriptionNodes())
            {
                if ( !this.dataNodes.containsKey(node.getNodeId()) )
                {
                    throw new MachineAdapterException("Node doesn't exist for this adapter"); //$NON-NLS-1$
                }

                subscriptionNodes.add(this.dataNodes.get(node.getNodeId()));
            }

            // Create new subscription.
            WorkshopDataSubscription newSubscription = new WorkshopDataSubscription(this, subscription.getName(),
                    subscription.getUpdateInterval(), subscriptionNodes);
            this.dataSubscriptions.put(newSubscription.getId(), newSubscription);
            new Thread(newSubscription).start();
            return newSubscription.getId();
        }

        return null;
    }

    /*
     * Remove data subscription from the list
     */
    @Override
	public synchronized void removeDataSubscription(UUID subscriptionId)
    {
        // Stop subscription, notify all subscribers, and remove subscription
        if ( this.dataSubscriptions.containsKey(subscriptionId) )
        {
            this.dataSubscriptions.get(subscriptionId).stop();
            this.dataSubscriptions.remove(subscriptionId);
        }
    }

    /**
     * get subscription given subscription id.
     */
    @Override
	public IDataSubscription getDataSubscription(UUID subscriptionId)
    {
        if ( this.dataSubscriptions.containsKey(subscriptionId) )
        {
            return this.dataSubscriptions.get(subscriptionId);
        }
        throw new MachineAdapterException("Subscription does not exist"); //$NON-NLS-1$ 
    }

    @SuppressWarnings("deprecation")
	@Override
	public synchronized void addDataSubscriptionListener(UUID dataSubscriptionId, IDataSubscriptionListener listener)
            throws MachineAdapterException
    {
        if ( this.dataSubscriptions.containsKey(dataSubscriptionId) )
        {
            this.dataSubscriptions.get(dataSubscriptionId).addDataSubscriptionListener(listener);
            return;
        }
        throw new MachineAdapterException("Subscription does not exist"); //$NON-NLS-1$	
    }

    @SuppressWarnings("deprecation")
	@Override
	public synchronized void removeDataSubscriptionListener(UUID dataSubscriptionId, IDataSubscriptionListener listener)
    {
        if ( this.dataSubscriptions.containsKey(dataSubscriptionId) )
        {
            this.dataSubscriptions.get(dataSubscriptionId).removeDataSubscriptionListener(listener);
        }
    }

    /*
     * #####################################
     * # Private methods #
     * #####################################
     */

    /**
     * Generates random nodes
     * 
     * @param count of nodes
     */
    private void createNodes(List<JsonDataNode> nodes)
    {
        for (JsonDataNode jsonNode:nodes)
        {
                WorskshopDataNodeIntel node = new WorskshopDataNodeIntel(this.uuid, jsonNode.getNodeName(),jsonNode.getNodeType(),jsonNode.getNodePin());
	            // Create a new node and put it in the cache.
	            this.dataNodes.put(node.getNodeId(), node);
            
        }
    }

    // Put data into data cache.
    /**
     * @param values list of values
     */
    protected void putData(List<PDataValue> values)
    {
        for (PDataValue value : values)
        {
            this.dataValueCache.put(value.getNodeId(), value);
        }
    }

	@Override
	public void addSubscriptionAdapterListener(ISubscriptionAdapterListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeSubscriptionAdapterListener(ISubscriptionAdapterListener arg0) {
		// TODO Auto-generated method stub
		
	}

}
