

Subscription Machine Adapter Sample - Implements ISubscriptionMachineAdapter interface and shows
	how to develop subscription functionality of machine adapter

At start up, the adapter creates data nodes that can be used to read/write data. The data for each
data node is provided from a random data generator, which runs on a separate thread. Data frequency
can be controlled using the update interval property. The latest data values are cached in memory
so that multiple clients can read same values.

The adapter also supports creation of subscriptions. A subscription is a logical grouping of
multiple nodes for the purpose of getting data updates in the same call. Any client can create a
subscription using data nodes provided by the adapter. Once a subscription is created, any client
can subscribe to data updates. Each subscription runs on a separate thread and supports multiple
subscribers.

To change the machine adapter settings/properties, such as adapter info, number of nodes, and update interval:
	1) Open the com.ge.dspmicro.sample.subscriptionmachineadapter.cfg configuration file located in 
		the <Predix-Machine>/configuration/machine folder
		
		There are 4 fields to modify: 
			# Data Update Interval in Milliseconds
			com.ge.dspmicro.sample.subscriptionmachineadapter.UpdateInterval=I"1000"

			# Number of Nodes
			com.ge.dspmicro.sample.subscriptionmachineadapter.NumberOfNodes=I"3"

			# Initial configured Subscriptions
			com.ge.dspmicro.sample.subscriptionmachineadapter.DataSubscriptions=["samplesub1", "samplesub2", "samplesub3" ]
			
			# Human Readable Adapter Name and Description Information
			com.ge.dspmicro.sample.subscriptionmachineadapter.Name="Subscription Adapter Sample"
			com.ge.dspmicro.sample.subscriptionmachineadapter.Description="This adapter generates random data"
