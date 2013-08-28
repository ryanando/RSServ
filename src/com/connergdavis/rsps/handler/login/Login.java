package com.connergdavis.rsps.handler.login;

import com.connergdavis.rsps.Peer;
import com.connergdavis.rsps.Server;
import com.connergdavis.rsps.handler.Handler;
import com.connergdavis.rsps.handler.InvalidStreamException;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * After a successful login request (or handshake, if you will)
 * has been conducted, the meat of the login stages is exchanged.
 * That is this.
 *
 * @author Conner Davis <connergdavis@gmail.com>
 */
public final class Login implements Handler
{

    /**
     * The username hash the client sent to us when we handled the
     * first login request.
     */
    private final int givenUsernameHash;
    private int[] sessionKeySet;

    public Login(int givenUsernameHash)
    {
        this.givenUsernameHash = givenUsernameHash;
    }

    @Override
    public boolean decode(ByteBuffer buf) throws InvalidStreamException
    {
        if (buf.remaining() < 3)
        {
            return false;
        }

        int totalLen = buf.getShort() & 0xFFFF;
        if (buf.remaining() != totalLen)
        {
            return false;
        }

        if (buf.getInt() != Server.REVISION)
        {
            throw new InvalidStreamException("Invalid client revision received");
        }

        buf.get();

        final int displayMode = buf.get() & 0xFF;
        final int screenWidth = buf.getShort() & 0xFFFF;
        final int screenHeight = buf.getShort() & 0xFFFF;

        // Commence large amount of information we don't need.
        for (int i = 0; i < 24; i++)
        {
            buf.get();
        }
        Peer.getNULString(buf);
        buf.getInt();
        buf.getInt();
        buf.getShort();
        for (int i = 0; i < 29; i++)
        {
            buf.getInt();
        }
        // End large amount of information we don't need.

        int rsaBlockLen = buf.get() & 0xFF;
        if (buf.remaining() < rsaBlockLen)
        {
            return false;
        }

        /*
         Everything from here on out is encrypted with RSA for two reasons:
            1. What good does a password do if anyone can sniff it easily?
            2. ISAAC ciphers are extremely important in preventing packet
                spoofing and sniffing, so we can't just send them out in the
                open (unfortunately, this is what most RSP servers do!!)
          */

        byte[] encrypted = new byte[rsaBlockLen];
        buf.get(encrypted);
        ByteBuffer encryptedBuf = ByteBuffer.wrap(new BigInteger(encrypted)
            .modPow(Server.RSA_EXPONENT, Server.RSA_MODULUS).toByteArray());

        if ((encryptedBuf.get() & 0xFF) != 10)
        {
            throw new InvalidStreamException("RSA block header wasn't 10");
        }

        long clientSessionKey = encryptedBuf.getLong();
        long serverSessionKey = encryptedBuf.getLong();

        sessionKeySet = new int[4];
        sessionKeySet[0] = (int) (clientSessionKey >> 32);
        sessionKeySet[1] = (int) clientSessionKey;
        sessionKeySet[2] = (int) (serverSessionKey >> 32);
        sessionKeySet[3] = (int) serverSessionKey;
        for (int i : sessionKeySet)
        {
            i += 50;
        }

        long usernameEncoded = encryptedBuf.getLong();
        long usernameHash = 31 & usernameEncoded >> 16;
        if (givenUsernameHash != usernameHash)
        {
            throw new InvalidStreamException("Mismatched username hashes");
        }
        String username = Peer.longToString(usernameEncoded);

        System.out.printf("Encrypted login request received [%s]\n", username);

        // TODO Check if player is already logged in
        // TODO Check if user limit in world has been met already

        String password = Peer.getNULString(encryptedBuf);
        // TODO Load saved player data

        return true;
    }

    @Override
    public ByteBuffer respond()
    {
        return null;
    }

    public int[] getSessionKeySet()
    {
        return sessionKeySet;
    }

}
