package com.connergdavis.rsps;

import net.openrs.cache.Cache;
import net.openrs.cache.ChecksumTable;
import net.openrs.cache.FileStore;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Entry point and place to store generic constants like the
 * game revision we're operating on, or the RSA cryption components.
 *
 * @author Conner Davis <connergdavis@gmail.com>
 */
public final class Server
{

    static ServerSocketChannel serverChannel;
    /**
     * Special thanks to devs of OpenRS cache library for this!
     */
    private static Cache cache;

    /**
     * The revision of the game client & cache we're working with.
     */
    public static final int REVISION = 562;

    /**
     * RSA private keypair used to decrypt the data sent by remote
     * peers during login that is really sensitive.
     */
    public static final BigInteger RSA_MODULUS = new BigInteger("");
    public static final BigInteger RSA_EXPONENT = new BigInteger("");

    public static void main(String[] args)
    {
        try
        {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(true);
            serverChannel.socket().bind(new InetSocketAddress(43594));

            System.out.println("Listening on port 43594.");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        try
        {
            cache = new Cache(FileStore.open(new File("C:/.jagex_cache_32/runescape")));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        new Thread(new Acceptor()).start();
    }

    public static Cache getCache()
    {
        return cache;
    }

}
