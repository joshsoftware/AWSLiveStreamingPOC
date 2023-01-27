package customMediaSource;

import com.amazonaws.kinesisvideo.client.mediasource.CameraMediaSourceConfiguration;
import com.amazonaws.kinesisvideo.client.mediasource.MediaSourceState;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.common.preconditions.Preconditions;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSource;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSourceConfiguration;
import com.amazonaws.kinesisvideo.internal.client.mediasource.MediaSourceSink;
import com.amazonaws.kinesisvideo.internal.mediasource.DefaultOnStreamDataAvailable;
import com.amazonaws.kinesisvideo.producer.StreamCallbacks;
import com.amazonaws.kinesisvideo.producer.StreamInfo;
import com.amazonaws.kinesisvideo.producer.Tag;
import com.github.sarxos.webcam.Webcam;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

import static com.amazonaws.kinesisvideo.producer.StreamInfo.NalAdaptationFlags.NAL_ADAPTATION_FLAG_NONE;
import static com.amazonaws.kinesisvideo.producer.StreamInfo.codecIdFromContentType;
import static com.amazonaws.kinesisvideo.util.StreamInfoConstants.*;

/**
 * custom media source for webcam
 */
public class CameraMediaSource implements MediaSource {
    private final String streamName;

    private CameraMediaSourceConfiguration mediaSourceConfiguration;
    private MediaSourceState mediaSourceState;
    private MediaSourceSink mediaSourceSink;
    private CameraFrameSource CameraFrameSource;
    private CompletableFuture<Boolean> future;
    private Webcam upWebCam;

    private static final byte[] AVCC_EXTRA_DATA = {
            (byte) 0x01, (byte) 0x42, (byte) 0x00, (byte) 0x1E, (byte) 0xFF, (byte) 0xE1, (byte) 0x00, (byte) 0x22,
            (byte) 0x27, (byte) 0x42, (byte) 0x00, (byte) 0x1E, (byte) 0x89, (byte) 0x8B, (byte) 0x60, (byte) 0x50,
            (byte) 0x1E, (byte) 0xD8, (byte) 0x08, (byte) 0x80, (byte) 0x00, (byte) 0x13, (byte) 0x88,
            (byte) 0x00, (byte) 0x03, (byte) 0xD0, (byte) 0x90, (byte) 0x70, (byte) 0x30, (byte) 0x00, (byte) 0x5D,
            (byte) 0xC0, (byte) 0x00, (byte) 0x17, (byte) 0x70, (byte) 0x5E, (byte) 0xF7, (byte) 0xC1, (byte) 0xF0,
            (byte) 0x88, (byte) 0x46, (byte) 0xE0, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0xCE,
            (byte) 0x1F, (byte) 0x20};

    public CameraMediaSource(@Nonnull final String streamName, CompletableFuture<Boolean> future) {
//        super(streamName);
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
    public StreamInfo getStreamInfo() throws KinesisVideoException {
        return new StreamInfo(VERSION_TWO,
                streamName,
                StreamInfo.StreamingType.STREAMING_TYPE_REALTIME,
                mediaSourceConfiguration.getEncoderMimeType(),
                NO_KMS_KEY_ID,
                RETENTION_ONE_HOUR,
                NOT_ADAPTIVE,
                MAX_LATENCY_ZERO,
                DEFAULT_GOP_DURATION,
                KEYFRAME_FRAGMENTATION,
                USE_FRAME_TIMECODES,
                RELATIVE_TIMECODES,
                REQUEST_FRAGMENT_ACKS,
                RECOVER_ON_FAILURE,
                codecIdFromContentType(mediaSourceConfiguration.getEncoderMimeType()),
                "test-track",
                DEFAULT_BITRATE,
                mediaSourceConfiguration.getFrameRate(),
                DEFAULT_BUFFER_DURATION,
                DEFAULT_REPLAY_DURATION,
                DEFAULT_STALENESS_DURATION,
                DEFAULT_TIMESCALE,
                RECALCULATE_METRICS,
                AVCC_EXTRA_DATA,
                new Tag[] {
                        new Tag("device", "Test Device"),
                        new Tag("stream", "Test Stream") },
                NAL_ADAPTATION_FLAG_NONE);
    }

    @Override
    public void initialize(@Nonnull final MediaSourceSink mediaSourceSink) throws KinesisVideoException {
//        super.initialize(mediaSourceSink);
        this.mediaSourceSink = mediaSourceSink;
    }

    @Override
    public void configure(@Nonnull final MediaSourceConfiguration configuration) {
//        super.configure(configuration);

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
