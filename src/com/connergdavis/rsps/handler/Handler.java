package com.connergdavis.rsps.handler;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Generic interface that sets the framework for our ability
 * to "decode" data from peers.  This process is essentially
 * converting the bytestream into something we can recognize,
 * and given a unique packet ID, we can always expect that data
 * to be specific by knowing what that peer is sending us.
 *
 * This process allows us to make use of what peers send us
 * because that info is often important!  Handlers are also allowed
 * to respond if they so choose -- this is very useful if we need
 * to keep data that was "decoded" and use it in a packet sent
 * downstream later on.
 *
 * @author Conner Davis <connergdavis@gmail.com>
 */
public interface Handler
{

    /**
     * "Decode" or convert the raw bytestream given to us into
     * objects we can understand.
     *
     * @param buf                       The buffer to read from.
     * @return                          Whether or not the stream was valid,
     *                                  meaning that all the data was OK.
     * @throws InvalidStreamException   In case a part of the stream is found to
     *                                  be bad, like the wrong client revision
     *                                  being sent.
     */
    public boolean decode(ByteBuffer buf) throws InvalidStreamException;

    /**
     * Compose a buffer of data as a response to the packet that we
     * just decoded.  Useful especially for packets received before
     * the peer has logged in.
     *
     * @return  Buffer of data, not flipped because it will be done by
     *          {@link com.connergdavis.rsps.Peer}.
     */
    public ByteBuffer respond();

}
