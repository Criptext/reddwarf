package com.sun.sgs.test.impl.service.data.store.net;

import com.sun.sgs.impl.service.data.store.DataStore;
import com.sun.sgs.impl.service.data.store.net.DataStoreClient;
import com.sun.sgs.impl.service.data.store.net.DataStoreServerImpl;
import com.sun.sgs.test.impl.service.data.store.TestPerformance;

public class TestClientPerformance extends TestPerformance {
    private static boolean externalServer =
	Boolean.getBoolean("test.externalServer");
    private static String serverHost =
	System.getProperty("test.serverHost", "localhost");
    private static int serverPort =
	Integer.getInteger("test.serverPort", 54321);
    
    /** The name of the DataStoreClient class. */
    private static final String DataStoreClientClassName =
	DataStoreClient.class.getName();

    /** The name of the DataStoreServerImpl class. */
    private static final String DataStoreServerImplClassName =
	DataStoreServerImpl.class.getName();

    DataStoreServerImpl server;

    public TestClientPerformance(String name) {
	super(name);
    }

    protected void tearDown() throws Exception {
	super.tearDown();
	try {
	    if (server != null) {
		server.shutdown();
	    }
	} catch (RuntimeException e) {
	    if (passed) {
		throw e;
	    } else {
		e.printStackTrace();
	    }
	}
    }

    /** Gets a DataStore using the default properties. */
    protected DataStore getDataStore() throws Exception {
	if (!externalServer) {
	    props.setProperty(DataStoreServerImplClassName + ".port", "0");
	    DataStoreServerImpl serverImpl = new DataStoreServerImpl(props);
	    server = serverImpl;
	    serverPort = serverImpl.getPort();
	}
	props.setProperty(DataStoreClientClassName + ".host", serverHost);
	props.setProperty(DataStoreClientClassName + ".port",
			  String.valueOf(serverPort));
	return new DataStoreClient(props);
    }
}
