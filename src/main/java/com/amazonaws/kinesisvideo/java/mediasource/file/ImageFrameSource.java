package com.amazonaws.kinesisvideo.java.mediasource.file;

import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.common.preconditions.Preconditions;
import com.amazonaws.kinesisvideo.internal.mediasource.OnStreamDataAvailable;
import com.amazonaws.kinesisvideo.producer.KinesisVideoFrame;
import com.github.sarxos.webcam.Webcam;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import utils.WebcamUtils;

import javax.annotation.concurrent.NotThreadSafe;
import java.awt.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.amazonaws.kinesisvideo.producer.FrameFlags.FRAME_FLAG_KEY_FRAME;
import static com.amazonaws.kinesisvideo.producer.FrameFlags.FRAME_FLAG_NONE;
import static com.amazonaws.kinesisvideo.producer.Time.HUNDREDS_OF_NANOS_IN_A_MILLISECOND;

/**
 * Frame source backed by local image files.
 */
@NotThreadSafe
public class ImageFrameSource {
    public static final int METADATA_INTERVAL = 8;
    private static final long FRAME_DURATION_20_MS = 20L;
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final int fps;
    private final ImageFileMediaSourceConfiguration configuration;

    private final int totalFiles;
    private OnStreamDataAvailable mkvDataAvailableCallback;
    private boolean isRunning = false;
    private int frameCounter;
    private final Log log = LogFactory.getLog(ImageFrameSource.class);
    private final String metadataName = "ImageLoop";
    private int metadataCount = 0;

    public ImageFrameSource(final ImageFileMediaSourceConfiguration configuration) {
        this.configuration = configuration;
        this.totalFiles = getTotalFiles(configuration.getStartFileIndex(), configuration.getEndFileIndex());
        this.fps = configuration.getFps();
    }

    private int getTotalFiles(final int startIndex, final int endIndex) {
        Preconditions.checkState(endIndex >= startIndex);
        return endIndex - startIndex + 1;
    }

    public void start() {
        if (isRunning) {
            throw new IllegalStateException("Frame source is already running");
        }

        isRunning = true;
        startFrameGenerator();
    }

    public void stop() {
        isRunning = false;
        stopFrameGenerator();
    }

    public void onStreamDataAvailable(final OnStreamDataAvailable onMkvDataAvailable) {
        this.mkvDataAvailableCallback = onMkvDataAvailable;
    }

    private void startFrameGenerator() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    generateFrameAndNotifyListener();
                } catch (final KinesisVideoException e) {
                    log.error("Failed to keep generating frames with Exception", e);
                }
            }
        });
    }

    private void generateFrameAndNotifyListener() throws KinesisVideoException {
        Webcam webcam =Webcam.getDefault();
        Dimension dimension = new Dimension(640, 480);
        webcam.setViewSize(dimension);
        while (isRunning) {
            if (mkvDataAvailableCallback != null) {
                mkvDataAvailableCallback.onFrameDataAvailable(createKinesisVideoFrameFromImage(frameCounter, webcam));
                if (isMetadataReady()) {
                    mkvDataAvailableCallback.onFragmentMetadataAvailable(metadataName + metadataCount,
                            Integer.toString(metadataCount++), false);
                }
            }

            frameCounter++;
            try {
                Thread.sleep(Duration.ofSeconds(1L).toMillis() / fps);
            } catch (final InterruptedException e) {
                log.error("Frame interval wait interrupted by Exception ", e);
            }
        }
        webcam.close();
    }

    private boolean isMetadataReady() {
        return frameCounter % METADATA_INTERVAL == 0;
    }

    private KinesisVideoFrame createKinesisVideoFrameFromImage(final long index, final Webcam webcam) {
//        final String filename = String.format(
//                configuration.getFilenameFormat(),
//                index % totalFiles + configuration.getStartFileIndex());
//        final Path path = Paths.get(configuration.getDir() + filename);
        final long currentTimeMs = System.currentTimeMillis();

        final int flags = isKeyFrame() ? FRAME_FLAG_KEY_FRAME : FRAME_FLAG_NONE;

        try {
//            final byte[] data = Files.readAllBytes(path);
            final byte[] data = WebcamUtils.getImageByteArrayFromWebCam(webcam);
            return new KinesisVideoFrame(
                    frameCounter,
                    flags,
                    currentTimeMs * HUNDREDS_OF_NANOS_IN_A_MILLISECOND,
                    currentTimeMs * HUNDREDS_OF_NANOS_IN_A_MILLISECOND,
                    FRAME_DURATION_20_MS * HUNDREDS_OF_NANOS_IN_A_MILLISECOND,
                    ByteBuffer.wrap(data));
        } catch (final Exception e) {
            log.error("Read file failed with Exception ", e);
        }

        return null;
    }

    private boolean isKeyFrame() {
        return frameCounter % configuration.getFps() == 0;
    }


    private void stopFrameGenerator() {
        executor.shutdown();
    }
}
