package com.connergdavis.rsps.handler.login;

import com.connergdavis.rsps.Server;
import com.connergdavis.rsps.handler.Handler;
import com.connergdavis.rsps.handler.InvalidStreamException;

import java.nio.ByteBuffer;

/**
 * @author Conner Davis <connergdavis@gmail.com>
 */
public final class UpdateServerHandshake implements Handler
{

    @Override
    public boolean decode(ByteBuffer buf) throws InvalidStreamException
    {
        if (buf.remaining() < 4)
        {
            return false;
        }

        if (buf.getInt() != Server.REVISION)
        {
            return false;
        }

        return true;
    }

    @Override
    public ByteBuffer respond()
    {
        return ByteBuffer.allocate(1).put((byte) 0);
    }

}
