package com.connergdavis.rsps.handler.login;

import com.connergdavis.rsps.handler.Handler;
import com.connergdavis.rsps.handler.InvalidStreamException;

import java.nio.ByteBuffer;

/**
 * The initial request/handshake/whatever that the client
 * starts during login.
 *
 * After this tiny data exchange comes the real stuff, but
 * we need this time to store the server's later important
 * ISAAC session key as well as make sure that the client
 * doesn't mismatch the username hash -- this is more
 * important for protection against bots than for practicality.
 *
 * @author Conner Davis <connergdavis@gmail.com>
 */
public final class LoginRequest implements Handler
{

    /**
     * The server's "session key" is an important component of
     * its ISAAC keypair which are used for in-game packet cryption.
     */
    private final long serverSessionKey;
    /**
     * Checked during the succeeding login data exchange to make
     * sure our peer hasn't experienced amnesia.
     */
    private int usernameHash;

    public LoginRequest()
    {
        serverSessionKey = ((long) (Math.random() * 99999999D) << 32) + (long) (Math.random() * 99999999D);
    }

    @Override
    public boolean decode(ByteBuffer buf) throws InvalidStreamException
    {
        usernameHash = buf.get() & 0xFF;
        return true;
    }

    @Override
    public ByteBuffer respond()
    {
        ByteBuffer out = ByteBuffer.allocate(9);
        out.put((byte) 0).putLong(serverSessionKey);
        return out;
    }

    public long getServerSessionKey()
    {
        return serverSessionKey;
    }

    public int getUsernameHash()
    {
        return usernameHash;
    }

}
