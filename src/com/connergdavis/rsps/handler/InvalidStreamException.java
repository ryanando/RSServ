package com.connergdavis.rsps.handler;

import com.connergdavis.rsps.Server;

/**
 * Thrown in the event that a part of a bytestream is found
 * to be critically bad, like if the client sends us a revision
 * that isn't {@link Server#REVISION}, meaning that we can't
 * operate with that peer properly.
 *
 * @author Conner Davis <connergdavis@gmail.com>
 */
public class InvalidStreamException extends Exception
{

    public InvalidStreamException(String message)
    {
        super(message);
    }

}
