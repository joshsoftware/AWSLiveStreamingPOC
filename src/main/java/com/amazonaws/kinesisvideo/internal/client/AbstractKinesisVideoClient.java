package com.amazonaws.kinesisvideo.internal.client;

import com.amazonaws.kinesisvideo.client.KinesisVideoClient;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSource;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import org.apache.logging.log4j.Logger;
import com.amazonaws.kinesisvideo.common.preconditions.Preconditions;
import com.amazonaws.kinesisvideo.producer.DeviceInfo;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import static com.amazonaws.kinesisvideo.common.preconditions.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Kinesis Video Client implementation which handles some of the common pieces
 * and delegates platform specifics to the implementations.
 */
@NotThreadSafe
public abstract class AbstractKinesisVideoClient implements KinesisVideoClient {

    /**
     * Stores the list of streams
     */
    protected final List<MediaSource> mMediaSources = new ArrayList<MediaSource>();

    /**
     * Whether the object has been initialized
     */
    protected boolean mIsInitialized = false;

    /**
     * Logging through this object
     */
    protected final Logger mLog;

    public AbstractKinesisVideoClient(@Nonnull final Logger log) {
        mLog = Preconditions.checkNotNull(log);
    }

    /**
     * Returns whether the client has been initialized
     */
    @Override
    public boolean isInitialized() {
        return mIsInitialized;
    }

    /**
     * Initializes the client object.
     */
    @Override
    public void initialize(@Nonnull final DeviceInfo deviceInfo) throws KinesisVideoException
    {
        mLog.info("Initializing Kinesis Video client");

        // Make sure we are not yet initialized
        checkState(!mIsInitialized, "Already initialized");

        // The actual initialization happens in the derived classes.
        mIsInitialized = true;
    }

    /**
     * Resumes the processing
     */
    @Override
    public void startAllMediaSources() throws KinesisVideoException {
        mLog.trace("Resuming Kinesis Video client");

        checkState(isInitialized(), "Must initialize first.");
        for (final MediaSource mediaSource : mMediaSources) {
            mediaSource.start();
        }
    }

    /**
     * Free media source's binding producer stream
     *
     * @param mediaSource media source binding to kinesis video producer stream to be freed
     * @throws KinesisVideoException if unable to free media source.
     */
    @Override
    public void freeMediaSource(@Nonnull final MediaSource mediaSource) throws KinesisVideoException {
        mMediaSources.remove(mediaSource);
        mediaSource.stop();
    }

    /**
     * Pauses the processing
     */
    @Override
    public void stopAllMediaSources() throws KinesisVideoException {
        mLog.trace("Pausing Kinesis Video client");

        if (!isInitialized()) {
            // Idempotent call
            return;
        }

        for (final MediaSource mediaSource : mMediaSources) {
            mediaSource.stop();
        }
    }

    /**
     * Stops the streams and frees/releases the underlying object
     */
    @Override
    public void free() throws KinesisVideoException {
        mLog.trace("Releasing Kinesis Video client");

        if (!isInitialized()) {
            // Idempotent call
            return;
        }

        for (final MediaSource mediaSource : mMediaSources) {
            if (!mediaSource.isStopped()) {
                mediaSource.stop();
            }

            mediaSource.free();
        }

        // Clean the list
        mMediaSources.clear();
    }

    /**
     * Register a media source. The media source will be binding to kinesis video producer stream
     * to send out data from media source.
     *
     * @param mediaSource media source binding to kinesis video producer stream
     * @throws KinesisVideoException
     */
    @Override
    public void registerMediaSource(@Nonnull final MediaSource mediaSource) throws KinesisVideoException {
        // The actual media source creation happens in the derived class
        mMediaSources.add(mediaSource);
    }

    @Override
    public void registerMediaSourceAsync(@Nonnull final MediaSource mediaSource) throws KinesisVideoException {
        mMediaSources.add(mediaSource);
    }

    /**
     * Un-Register a media source. The media source will stop binding to kinesis video producer stream
     * and it cannot send data via producer stream afterwards until register again.
     *
     * @param mediaSource media source to stop binding to kinesis video producer stream
     * @throws KinesisVideoException
     */
    @Override
    public void unregisterMediaSource(@Nonnull final MediaSource mediaSource) throws KinesisVideoException {
        mMediaSources.remove(mediaSource);
        mediaSource.stop();
    }
}
