package com.connergdavis.rsps;

import com.connergdavis.rsps.handler.Handler;
import com.connergdavis.rsps.handler.InvalidStreamException;
import com.connergdavis.rsps.handler.login.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Representation of an individual 'peer' on the network,
 * or client, or what have you.  Its purpose is to let us
 * store instance data about each client as well as serve
 * as our ability to communicate with clients.
 *
 * @author Conner Davis <connergdavis@gmail.com>
 */
public final class Peer implements Runnable
{

    private SocketChannel channel;
    /**
     * Constantly flowing buffer of data that gets accessed
     * whenever this peer sends something to us.
     */
    private ByteBuffer in = ByteBuffer.allocate(8192);
    /**
     * In the case that TCP decides to ruthlessly fragment packets,
     * this helps us by letting us keep track of the current packet's
     * ID so that when all the data finally gets to us, we haven't
     * lost it.
     */
    private int currentPacketId = -1;
    /**
     * Keeps track of two states: pre-login or logging in, and logged in.
     * The difference is in the packets, as well as the fact that the ISAAC
     * cipher isn't applied to LOGIN packets, whereas it will be for LOGGED_IN
     * packets.
     */
    private ConnectionStage connectionStage = ConnectionStage.LOGIN;

    /**
     * Pair of ISAAC stream ciphers which are used to encrypt the opcodes
     * of packets sent over the stream to prevent people from sniffing packets.
     */
    private IsaacCipher inCipher;
    private IsaacCipher outCipher;

    /**
     * Required in order to convert the player's username from long
     * form to string form during login.
     */
    private static final char[] VALID_CHARS = {
        '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0',
        '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    /**
     * Only temporarily kept during login, this is just another thing
     * to check and make sure isn't wrong when we get it from the client
     * during login.
     */
    private int usernameHash;

    Peer(SocketChannel channel)
    {
        this.channel = channel;
    }

    @Override
    public void run()
    {
        int available;
        Handler currentHandler;

        /*
        Basically, we'll block (continuously loop) on this thread and constantly
        attempt to read data from the remote peer.

        The way this works in a nutshell is that we assume we'll always start with a
        packet ID from the beginning of the stream (always true.)

        Then, we set that packet ID to memory so we can use it when the rest of the
        actual packet's payload of data has been sent.

        The reason I kept track of "available" bytes is because I continue to reposition
        the ByteBuffer to the beginning of the packet's payload whenever we attempt to
        decode it -- this allows us to keep adding data to the buffer as it's sent over
        TCP.
         */
        while (true)
        {
            try
            {
                available = 0;
                while ((available += channel.read(in)) != -1)
                {
                    if (currentPacketId == -1)
                    {
                        in.flip();
                        currentPacketId = in.get() & 0xFF;
                        in.mark();
                    }

                    in.reset();
                    if ((currentHandler = getHandlerById(channel, currentPacketId)).decode(in))
                    {
                        handleByHandler(currentHandler);
                        try
                        {
                            ByteBuffer response = currentHandler.respond();
                            response.flip();
                            channel.write(response);
                        }
                        catch (NullPointerException e)
                        {
                            // Ignore, this happens if the handler has nothing to respond with.
                        }

                        currentPacketId = -1;
                        available = 0;
                        in.clear();
                    }
                    else
                    {
                        in.position(available);
                    }
                }
            }
            catch (InvalidStreamException ise)
            {
                ise.printStackTrace();
                break;
            }
            catch (IOException ioe)
            {
                break;
            }
        }

        try
        {
            /*
             In the situation that some error is found while reading/writing,
             be sure to also close the channel before thread dies.
              */
            channel.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Based on our knowledge of each packet's purpose based on its
     * ID, try to find the associated {@link Handler} that will be able
     * to (try to) process the data in our buffer, assuming that that
     * data will be encoded in the expected format, again, based on the
     * packet ID.
     *
     * @param channel                   This peer's socket channel.
     * @param packetId                  The unique ID of the packet.
     * @return                          The handler associated with this packet, if any.
     * @throws InvalidStreamException   In the event that a very bad problem is found in the stream,
     *                                  e.g. the wrong client revision is sent in which case we can't
     *                                  continue trying to read from this client.
     */
    private Handler getHandlerById(SocketChannel channel, int packetId) throws InvalidStreamException
    {
        System.out.printf("Trying to find handler for packet [%d]\n", packetId);
        switch (connectionStage)
        {
            case LOGIN:
                switch (packetId)
                {
                    case 15:    // Update server
                        return new UpdateServerHandshake();
                    case 6:
                        return new UpdateServerClientInitiated();
                    case 2:     // Client is logged in
                    case 3:     // Client is logged out
                        return new UpdateServerClientStatus();
                    case 0:
                    case 1:     // Update server priority request
                        return new UpdateServerRequest(channel, packetId);
                    case 4:
                        return new UpdateServerNewEncryptionByte();

                    case 14:    // Login handshake
                        return new LoginRequest();
                    case 16:    // Verified login, really going to exchange data this time.
                    case 18:
                        return new Login(usernameHash);
                }
                break;
            case LOGGED_IN:
                break;
        }
        return null;
    }

    /**
     * Does any sort of backend logic stuff that may need to be done
     * after decoding.
     *
     * Yes, I'm hilarious.
     *
     * @param handler   The handler to.. handle.
     */
    private void handleByHandler(Handler handler)
    {
        if (handler instanceof LoginRequest)
        {
            usernameHash = ((LoginRequest) handler).getUsernameHash();
        }
        else if (handler instanceof Login)
        {
            // Initialize the ISAAC ciphers for in-game packets
            int[] keySet = ((Login) handler).getSessionKeySet();
            inCipher = new IsaacCipher(keySet);
            outCipher = new IsaacCipher(keySet);

            // Prepare this peer for in-game packets
            connectionStage = ConnectionStage.LOGGED_IN;

            System.out.printf("Remote peer from [%s] logged in and ready for in-game packets.",
                channel.socket().getInetAddress().toString());
        }
    }

    /**
     * Read a NUL-terminated string from a bytestream.
     *
     * @param in    The given bytestream to read from.
     * @return      String of data composed from each char in the bytestream.
     */
    public static String getNULString(ByteBuffer in)
    {
        StringBuilder str = new StringBuilder();
        byte next;
        while ((next = in.get()) != 0)
        {
            str.append((char) next);
        }
        return str.toString();
    }

    /**
     * Converts a long to a string!  Important in login to be able
     * to read the given username.
     *
     * @param l You guessed it, the long we'll be converting today.
     * @return  Again, you probably figured this one out -- our new string!!
     */
    public static String longToString(long l)
    {
        if (l <= 0L || l >= 0x5b5b57f8a98a5dd1L)
        {
            return null;
        }
        if (l % 37L == 0L)
        {
            return null;
        }
        int i = 0;
        char ac[] = new char[12];
        while (l != 0L) {
            long l1 = l;
            l /= 37L;
            ac[11 - i++] = VALID_CHARS[(int) (l1 - l * 37L)];
        }
        return new String(ac, 12 - i, i);
    }

    /**
     * Define the progress that the user has made in fully logging into the game.
     * The reason these stages are important is because, for example, the packet
     * with ID 15 when the user first connects to the server varies from that same
     * packet ID when the user has already logged in and loaded the game.
     *
     * I kept it simple since each packet exchanged during login is unique, and there
     * are very few.  After that, however, it is important that each peer who has
     * successfully logged into the game be updated to {@link ConnectionStage#LOGGED_IN}
     * state.
     */
    private enum ConnectionStage
    {

        LOGIN,
        LOGGED_IN

    }

}
