package com.connergdavis.rsps;

import java.nio.channels.SocketChannel;

/**
 * Constantly looping thread that tries to accept any
 * new clients that want to.
 *
 * @author Conner Davis <connergdavis@gmail.com>
 */
final class Acceptor implements Runnable
{

    @Override
    public void run()
    {
        SocketChannel channel;

        while (true)
        {
            try
            {
                channel = Server.serverChannel.accept();

                if (channel != null)
                {
                    // We had a new peer connect to us, so create their thread and start reading from them.
                    channel.configureBlocking(true);
                    new Thread(new Peer(channel)).start();
                }
            }
            catch (Exception e)
            {
                // This is fatal, so make sure to close the server.
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

}
