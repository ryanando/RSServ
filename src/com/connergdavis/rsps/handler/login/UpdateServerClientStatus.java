package com.connergdavis.rsps.handler.login;

import com.connergdavis.rsps.handler.Handler;
import com.connergdavis.rsps.handler.InvalidStreamException;

import java.nio.ByteBuffer;

/**
 * @author Conner Davis <connergdavis@gmail.com>
 */
public final class UpdateServerClientStatus implements Handler
{

    @Override
    public boolean decode(ByteBuffer buf) throws InvalidStreamException
    {
        return true;
    }

    @Override
    public ByteBuffer respond()
    {
        return null;
    }
}
