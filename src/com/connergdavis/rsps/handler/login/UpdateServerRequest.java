package com.connergdavis.rsps.handler.login;

import com.connergdavis.rsps.Server;
import com.connergdavis.rsps.handler.Handler;
import com.connergdavis.rsps.handler.InvalidStreamException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author Conner Davis <connergdavis@gmail.com>
 */
public final class UpdateServerRequest implements Handler
{

    private int cacheId = -1;
    private int fileId = -1;
    private SocketChannel channel;
    private int priority;

    public UpdateServerRequest(SocketChannel channel, int priority)
    {
        this.channel = channel;
        this.priority = priority;
    }

    @Override
    public boolean decode(ByteBuffer buf) throws InvalidStreamException
    {
        if (buf.remaining() < 3)
        {
            return false;
        }

        this.cacheId = buf.get() & 0xFF;
        this.fileId = buf.getShort();
        return true;
    }

    @Override
    public ByteBuffer respond()
    {
        try
        {
            if (cacheId == 255 && fileId == 255)
            {
                ByteBuffer out;
                ByteBuffer uKeys = Server.getCache().createChecksumTable().encode();
                out = ByteBuffer.allocate(uKeys.limit() + 8);
                out.put((byte) 0xFF).putShort((short) 0xFF).put((byte) 0).putInt(uKeys.limit()).put(uKeys);
                return out;
            }
            else
            {
                return null;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

}
