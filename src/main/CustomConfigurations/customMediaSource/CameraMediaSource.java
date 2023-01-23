package customMediaSource;

import com.amazonaws.kinesisvideo.client.mediasource.CameraMediaSourceConfiguration;
import com.amazonaws.kinesisvideo.client.mediasource.MediaSourceState;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.common.preconditions.Preconditions;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSourceConfiguration;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSourceSink;
import com.amazonaws.kinesisvideo.internal.mediasource.DefaultOnStreamDataAvailable;
import com.amazonaws.kinesisvideo.internal.mediasource.multitrack.MultiTrackMediaSource;
import com.amazonaws.kinesisvideo.producer.StreamCallbacks;
import com.github.sarxos.webcam.Webcam;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * custom media source for webcam
 */
public class CameraMediaSource extends MultiTrackMediaSource {
    private final String streamName;

    private CameraMediaSourceConfiguration mediaSourceConfiguration;
    private MediaSourceState mediaSourceState;
    private MediaSourceSink mediaSourceSink;
    private CameraFrameSource CameraFrameSource;
    private CompletableFuture<Boolean> future;
    private Webcam upWebCam;

    public CameraMediaSource(@Nonnull final String streamName, CompletableFuture<Boolean> future) {
        super(streamName);
        this.streamName = streamName;
        this.future = future;
    }

    public CameraMediaSource(@Nonnull final String streamName) {
        this(streamName, new CompletableFuture<Boolean>());
    }

    @Override
    public MediaSourceState getMediaSourceState() {
        return mediaSourceState;
    }

    @Override
    public MediaSourceConfiguration getConfiguration() {
        return mediaSourceConfiguration;
    }

    @Override
    public void initialize(@Nonnull final MediaSourceSink mediaSourceSink) throws KinesisVideoException {
        super.initialize(mediaSourceSink);
        this.mediaSourceSink = mediaSourceSink;
    }

    @Override
    public void configure(@Nonnull final MediaSourceConfiguration configuration) {
        super.configure(configuration);

        Preconditions.checkState(this.mediaSourceConfiguration == null);

        if (!(configuration instanceof CameraMediaSourceConfiguration)) {
            throw new IllegalStateException(
                    "Configuration must be an instance of CameraMediaSourceConfiguration");
        }
        this.mediaSourceConfiguration = (CameraMediaSourceConfiguration) configuration;
    }

    @Override
    public void start() throws KinesisVideoException {
        mediaSourceState = MediaSourceState.RUNNING;
        CameraFrameSource = new CameraFrameSource(mediaSourceConfiguration);
        CameraFrameSource.onStreamDataAvailable(new DefaultOnStreamDataAvailable(mediaSourceSink));
        CameraFrameSource.start();
    }

    @Override
    public void stop() throws KinesisVideoException {
        if (CameraFrameSource != null) {
            CameraFrameSource.stop();
        }

        try {
            mediaSourceSink.getProducerStream().stopStreamSync();
        } finally {
            mediaSourceState = MediaSourceState.STOPPED;
            future.complete(true);
        }
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public void free() throws KinesisVideoException {
        // No-op
    }

    @Override
    public MediaSourceSink getMediaSourceSink() {
        return mediaSourceSink;
    }

    @Nullable
    @Override
    public StreamCallbacks getStreamCallbacks() {
        return null;
    }

    public void setUpWebCam(Webcam upWebCam) {
        this.upWebCam = upWebCam;
    }

    public Webcam getUpWebCam() {
        return upWebCam;
    }
}
