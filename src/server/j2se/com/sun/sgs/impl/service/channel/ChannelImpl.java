package com.sun.sgs.impl.service.channel;

import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelListener;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.service.ClientSessionService;
import com.sun.sgs.service.DataService;
import com.sun.sgs.service.TaskService;
import com.sun.sgs.impl.service.channel.ChannelServiceImpl.Context;
import com.sun.sgs.impl.service.session.SgsProtocol;
import com.sun.sgs.impl.util.HexDumper;
import com.sun.sgs.impl.util.LoggerWrapper;
import com.sun.sgs.impl.util.MessageBuffer;
import com.sun.sgs.kernel.KernelRunnable;
import com.sun.sgs.service.SgsClientSession;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Channel implementation for use within a single transaction
 * specified by the context passed during construction.
 */
final class ChannelImpl implements Channel, Serializable {

    /** The serialVersionUID for this class. */
    private final static long serialVersionUID = 1L;
    
    /** The logger for this class. */
    private final static LoggerWrapper logger =
	new LoggerWrapper(
	    Logger.getLogger(ChannelImpl.class.getName()));

    private final static byte[] EMPTY_ID = new byte[0];

    /** Transaction-related context information. */
    private final Context context;

    /** Persistent channel state. */
    private final ChannelState state;

    /** Flag that is 'true' if this channel is closed. */
    private boolean channelClosed = false;

    /**
     * Constructs an instance of this class with the specified context
     * and channel state.
     *
     * @param context a context
     * @param state a channel state
     */
    ChannelImpl(Context context, ChannelState state) {
	if (logger.isLoggable(Level.FINER)) {
	    logger.log(Level.FINER, "Created ChannelImpl context:{0} state:{1}",
		       context, state);
	}
	this.state =  state;
	this.context = context;
    }

    /* -- Implement Channel -- */
    
    /** {@inheritDoc} */
    public String getName() {
	checkContext();
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST, "getName returns {0}", state.name);
	}
	return state.name;
    }

    /** {@inheritDoc} */
    public Delivery getDeliveryRequirement() {
	checkContext();
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST,
		       "getDeliveryRequirement returns {0}", state.delivery);
	}
	return state.delivery;
    }

    /** {@inheritDoc} */
    public void join(final ClientSession session, ChannelListener listener) {
	try {
	    checkClosed();
	    if (session == null) {
		throw new NullPointerException("null session");
	    }
	    if (listener != null && !(listener instanceof Serializable)) {
		throw new IllegalArgumentException("listener not serializable");
	    }
	    if (!(session instanceof SgsClientSession)) {
		if (logger.isLoggable(Level.SEVERE)) {
		    logger.log(
			Level.SEVERE,
			"join: session does not implement" +
			"SgsClientSession:{0}", session);
		}
		throw new IllegalArgumentException(
		    "unexpected ClientSession type: " + session);
	    }
	    if (state.hasSession(session)) {
		return;
	    }
	    
	    context.getService(DataService.class).markForUpdate(state);
	    state.addSession(session, listener);
	    int nameSize = MessageBuffer.getSize(state.name);
	    MessageBuffer buf = new MessageBuffer(3 + nameSize);
	    buf.putByte(SgsProtocol.VERSION).
		putByte(SgsProtocol.CHANNEL_SERVICE).
		putByte(SgsProtocol.CHANNEL_JOIN).
		putString(state.name);
	    sendProtocolMessageOnCommit(session, buf.getBuffer());
	    
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST, "join session:{0} returns", session);
	    }
	    
	} catch (RuntimeException e) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.logThrow(Level.FINEST, e, "leave throws");
	    }
	    throw e;
	}
    }

    /** {@inheritDoc} */
    public void leave(final ClientSession session) {
	try {
	    checkClosed();
	    if (session == null) {
		throw new NullPointerException("null client session");
	    }
	    if (!(session instanceof SgsClientSession)) {
		if (logger.isLoggable(Level.SEVERE)) {
		    logger.log(
			Level.SEVERE,
			"join: session does not implement " +
			"SgsClientSession:{0}", session);
		}
		throw new IllegalArgumentException(
		    "unexpected ClientSession type: " + session);
	    }

	    if (!state.hasSession(session)) {
		return;
	    }
	    
	    context.getService(DataService.class).markForUpdate(state);
	    state.removeSession(session);
	
	    int nameSize = MessageBuffer.getSize(state.name);
	    MessageBuffer buf = new MessageBuffer(3 + nameSize);
	    buf.putByte(SgsProtocol.VERSION).
		putByte(SgsProtocol.CHANNEL_SERVICE).
		putByte(SgsProtocol.CHANNEL_LEAVE).
		putString(state.name);
	    sendProtocolMessageOnCommit(session, buf.getBuffer());
	    
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST, "leave session:{0} returns", session);
	    }
	    
	} catch (RuntimeException e) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.logThrow(Level.FINEST, e, "leave throws");
	    }
	    throw e;
	}
    }

    /** {@inheritDoc} */
    public void leaveAll() {
	try {
	    checkClosed();
	    if (!state.hasSessions()) {
		return;
	    }
	    context.getService(DataService.class).markForUpdate(state);
	    state.removeAll();

	    final Collection<ClientSession> sessions = getSessions();

	    int nameSize = MessageBuffer.getSize(state.name);
	    MessageBuffer buf = new MessageBuffer(3 + nameSize);
	    buf.putByte(SgsProtocol.VERSION).
		putByte(SgsProtocol.CHANNEL_SERVICE).
		putByte(SgsProtocol.CHANNEL_LEAVE).
		putString(state.name);
	    byte[] message = buf.getBuffer();
		    
	    for (ClientSession session : sessions) {
		sendProtocolMessageOnCommit(session, message);
	    }
	    
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST, "leaveAll returns");
	    }
	} catch (RuntimeException e) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.logThrow(Level.FINEST, e, "leave throws");
	    }
	    throw e;
	}
    }

    /** {@inheritDoc} */
    public boolean hasSessions() {
	checkClosed();
	boolean hasSessions = state.hasSessions();
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST, "hasSessions returns {0}", hasSessions);
	}
	return hasSessions;
    }

    /** {@inheritDoc} */
    public Collection<ClientSession> getSessions() {
	checkClosed();
	Collection<ClientSession> sessions =
	    Collections.unmodifiableCollection(state.getSessions());
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST, "getSessions returns {0}", sessions);
	}
	return sessions;
    }

    /** {@inheritDoc} */
    public void send(byte[] message) {
	try {
	    checkClosed();
	    if (message == null) {
		throw new NullPointerException("null message");
	    }
	    sendToClients(EMPTY_ID, state.getSessions(), message,
			  context.nextSequenceNumber());
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST, "send message:{0} returns", message);
	    }
	    
	} catch (RuntimeException e) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.logThrow(
		    Level.FINEST, e, "send message:{0} throws", message);
	    }
	    throw e;
	}
    }

    /** {@inheritDoc} */
    public void send(ClientSession recipient, byte[] message) {
	try {
	    checkClosed();
	    if (recipient == null) {
		throw new NullPointerException("null recipient");
	    } else if (message == null) {
		throw new NullPointerException("null message");
	    }
	
	    Collection<ClientSession> sessions = new ArrayList<ClientSession>();
	    sessions.add(recipient);
	    sendToClients(EMPTY_ID, sessions, message,
			  context.nextSequenceNumber());
	    
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(
		    Level.FINEST, "send recipient: {0} message:{1} returns",
		    recipient, message);
	    }
	    
	} catch (RuntimeException e) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.logThrow(
		    Level.FINEST, e, "send recipient: {0} message:{1} throws",
		    recipient, message);
	    }
	    throw e;
	}
    }

    /** {@inheritDoc} */
    public void send(Collection<ClientSession> recipients,
		     byte[] message)
    {
	try {
	    checkClosed();
	    if (recipients == null) {
		throw new NullPointerException("null recipients");
	    } else if (message == null) {
		throw new NullPointerException("null message");
	    }

	    if (!recipients.isEmpty()) {
		sendToClients(EMPTY_ID, recipients, message,
			      context.nextSequenceNumber());
	    }
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(
		    Level.FINEST, "send recipients: {0} message:{1} returns",
		    recipients, message);
	    }
	
	} catch (RuntimeException e) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.logThrow(
		    Level.FINEST, e, "send recipients: {0} message:{1} throws",
		    recipients, message);
	    }
	    throw e;
	}
    }

    /** {@inheritDoc} */
    public void close() {
	checkContext();
	channelClosed = true;
	context.removeChannel(state.name);
	if (logger.isLoggable(Level.FINEST)) {
	    logger.log(Level.FINEST, "close returns");
	}
    }

    /* -- Implement Object -- */

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
	return
	    (this == obj) ||
	    (obj.getClass() == this.getClass() &&
	     state.equals(((ChannelImpl) obj).state));
    }

    /** {@inheritDoc} */
    public int hashCode() {
	return state.name.hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
	return getClass().getName() + "[" + state.name + "]";
    }

    /* -- Serialization methods -- */

    private Object writeReplace() {
	return new External(state.name);
    }

    /**
     * Represents the persistent represntation for a channel (just its name).
     */
    private final static class External implements Serializable {

	private final static long serialVersionUID = 1L;

	private final String name;

	External(String name) {
	    this.name = name;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
	    out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in)
	    throws IOException, ClassNotFoundException
	{
	    in.defaultReadObject();
	}

	private Object readResolve() throws ObjectStreamException {
	    try {
		ChannelManager cm = AppContext.getChannelManager();
		Channel channel = cm.getChannel(name);
		return channel;
	    } catch (RuntimeException e) {
		throw (InvalidObjectException)
		    new InvalidObjectException(e.getMessage()).initCause(e);
	    }
	}
    }

    /* -- other methods and classes -- */

    /**
     * Checks that this channel's context is currently active,
     * throwing TransactionNotActiveException if it isn't.
     */
    private void checkContext() {
	ChannelServiceImpl.checkContext(context);
    }

    /**
     * Checks the context, and then checks that this channel is not
     * closed, throwing an IllegalStateException if the channel is
     * closed.
     */
    private void checkClosed() {
	checkContext();
	if (channelClosed) {
	    throw new IllegalStateException("channel is closed");
	}
    }

    /**
     * Notifies the appropriate channel listeners that the specified
     * message was sent by the specified sender.
     */
    void notifyListeners(byte[] senderId, byte[] message) {
	checkContext();
	if (channelClosed) {
	    throw new IllegalStateException("channel is closed");
	}
	
	ClientSession senderSession =
	    context.getService(ClientSessionService.class).
	        getClientSession(senderId);
	if (senderSession == null) {
	    /*
	     * Sending session has disconnected, so return.
	     */
	    // TBD: should this notify the channel-wide listener?
	    return;
	}
	
	// Notify per-channel listener.
	ChannelListener listener = state.getListener();
	if (listener != null) {
	    listener.receivedMessage(this, senderSession, message);
	}

        // Notify per-session listener.
        listener = state.getListener(senderSession);
        if (listener != null) {
            listener.receivedMessage(this, senderSession, message);
        }
    }

    /**
     * Forwards message to recipient sessions.
     */
    void forwardMessage(final byte[] senderId,
            final Collection<byte[]> recipientIds,
            final byte[] message, final long seq)
    {
        // Build the list of recipients
        final Collection<ClientSession> recipients;
        if (recipientIds.size() == 0) {
            recipients = state.getSessionsExcludingId(senderId);
        } else {
            recipients = new ArrayList<ClientSession>();
            for (byte[] sessionId : recipientIds) {
                ClientSession session =
                    context.getService(ClientSessionService.class).
		        getClientSession(sessionId);
                // Skip the sender and any disconnected sessions
                if ((session != null) &&
                    (! senderId.equals(session.getSessionId())))
                {
                    recipients.add(session);
                }
            }
        }

        /*
         * If there are no connected sessions other than the sender,
         * we don't need to send anything.
         */
        if (recipients.isEmpty()) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(
                        Level.FINEST,
                "no recipients except sender");
            }
            return;
        }
        
        final String name = state.name;
        final Delivery delivery = state.delivery;
        
        context.channelService.nonDurableTaskScheduler.
            scheduleNonTransactionalTaskOnCommit(
                new KernelRunnable() {
                    public void run() throws Exception {
                        forwardToSessions(
                            name, senderId, recipients, message, seq, delivery);
                    }
                });
    }

    /**
     * Forward a message to the given recipients.
     */
    static void forwardToSessions(String name,
            byte[] senderId,
            Collection<ClientSession> recipients,
            byte[] message,
            long seq,
            Delivery delivery)
    {
        try {

            MessageBuffer protocolMessage =
                getChannelMessage(name, senderId, message, seq);

            for (ClientSession session : recipients) {
                ((SgsClientSession) session).sendMessage(
                        protocolMessage.getBuffer(), delivery);
            }

        } catch (RuntimeException e) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logThrow(
                        Level.FINER, e,
                        "name:{0}, message:{1} throws",
                        name, HexDumper.format(message));
            }
            throw e;
        }
    }
    
    static MessageBuffer getChannelMessage(String name, byte[] senderId,
            byte[] message, long seq)
    {
        int nameLen = MessageBuffer.getSize(name);
        MessageBuffer buf =
            new MessageBuffer(15 + nameLen + senderId.length +
                    message.length);
        buf.putByte(SgsProtocol.VERSION).
            putByte(SgsProtocol.CHANNEL_SERVICE).
            putByte(SgsProtocol.CHANNEL_MESSAGE).
            putString(name).
            putLong(seq).
            putShort(senderId.length).
            putBytes(senderId).
            putShort(message.length).
            putBytes(message);

        return buf;
    }
    
    /**
     * Schedules a non-durable, non-transactional task.
     */
    private void scheduleNonTransactionalTask(KernelRunnable task) {
	context.getService(TaskService.class).scheduleNonDurableTask(task);
    }

    /**
     * Send a protocol message to the specified session, logging (but
     * not throwing) any exception.
     */
    private void sendProtocolMessage(ClientSession session, byte[] message) {
	try {
	    ((SgsClientSession) session).sendMessage(message, state.delivery);
	} catch (RuntimeException e) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.logThrow(
		    Level.FINEST, e,
		    "sendProtcolMessage session:{0} message:{1} throws",
		    session, message);
	    }
	    // eat exception
	}
    }
    
    /**
     * Send a protocol message to the specified session when the
     * transaction commits, logging (but not throwing) any exception.
     */
    private void sendProtocolMessageOnCommit(
	ClientSession session, byte[] message)
    {
        try {
            ((SgsClientSession) session).
		sendMessageOnCommit(message, state.delivery);
        } catch (RuntimeException e) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.logThrow(
                    Level.FINEST, e,
                    "sendProtcolMessageOnCommit session:{0} message:{1} throws",
                    session, message);
            }
            // eat exception
        }
    }

    /**
     * Task for sending a message to a set of clients.
     */
    private void sendToClients(byte[] senderId,
			       Collection<ClientSession> sessions,
			       byte[] message,
			       long sequenceNumber)
    {
	Collection<byte[]> clients = new ArrayList<byte[]>();
	for (ClientSession session : sessions) {
	    clients.add(session.getSessionId());
	}
	MessageBuffer buf =
	    new MessageBuffer(15 + senderId.length + message.length);
	buf.putByte(SgsProtocol.VERSION).
	    putByte(SgsProtocol.CHANNEL_SERVICE).
	    putByte(SgsProtocol.CHANNEL_MESSAGE).
	    putLong(sequenceNumber).
	    putShort(senderId.length).
	    putBytes(senderId).
	    putShort(message.length).
	    putBytes(message);

	byte[] protocolMessage = buf.getBuffer();
	    
	for (byte[] sessionId : clients) {
	    SgsClientSession session = 
		context.getService(ClientSessionService.class).
		    getClientSession(sessionId);
	    if (session != null && session.isConnected()) {
		session.sendMessage(protocolMessage, state.delivery);
	    }
	}
    }
}
