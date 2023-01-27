package customMediaSource;

import com.amazonaws.kinesisvideo.client.mediasource.CameraMediaSourceConfiguration;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.common.preconditions.Preconditions;
import com.amazonaws.kinesisvideo.internal.mediasource.OnStreamDataAvailable;
import com.amazonaws.kinesisvideo.producer.KinesisVideoFrame;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.util.ImageUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.concurrent.NotThreadSafe;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.amazonaws.kinesisvideo.producer.FrameFlags.FRAME_FLAG_KEY_FRAME;
import static com.amazonaws.kinesisvideo.producer.Time.HUNDREDS_OF_NANOS_IN_A_MILLISECOND;
import static com.amazonaws.kinesisvideo.util.StreamInfoConstants.FRAME_DURATION_0_MS;
import static com.amazonaws.kinesisvideo.util.StreamInfoConstants.VIDEO_TRACK_ID;

/**
 * Frame source backed by reading image files from webcam.
 */
@NotThreadSafe
public class CameraFrameSource {
    private static final String DELIMITER = "-";
    private static final int INFO_LENGTH = 4;
    private static final String VIDEO_TYPE = "video";
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final int fps;
    private final CameraMediaSourceConfiguration configuration;

    private OnStreamDataAvailable mkvDataAvailableCallback;
    private volatile boolean isRunning = false;
    private final Log log = LogFactory.getLog(CameraFrameSource.class);
    private long durationInMillis = 0;
    private int frameIndex = 0;
    private long frameStartMillis = 0;
    private List<String> fileNames = new ArrayList<>();

    public CameraFrameSource(final CameraMediaSourceConfiguration configuration) {
        this.configuration = configuration;
        this.fps = configuration.getFrameRate();
    }

    private void getTotalFiles(final File fileDirectory) {
        Preconditions.checkState(fileDirectory.isDirectory());

        final String[] fileNameList = fileDirectory.list();
        fileNames = Arrays.asList(fileNameList == null ? new String[0] : fileNameList);
        fileNames.sort((s1, s2) ->
                (Long.parseLong(s1.split(DELIMITER)[0]) - Long.parseLong(s2.split(DELIMITER)[0]) > 0 ? 1 : -1));
        frameStartMillis = configuration.getIsAbsoluteTimecode()
                ? Duration.ofNanos(Long.parseLong(fileNames.get(0).split(DELIMITER)[0])).toMillis() : 0;
        durationInMillis =
                Duration.ofNanos(Long.parseLong(fileNames.get(fileNames.size() - 1).split(DELIMITER)[0])).toMillis()
                        + Duration.ofSeconds(1L).toMillis() - frameStartMillis;
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

    private void generateFrameAndNotifyListener() throws KinesisVideoException{
        final Webcam webcam = Webcam.getDefault();
        webcam.open();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (isRunning) {
//            custom implementation for the frames from webcam to read for stream

            try {
                if (mkvDataAvailableCallback != null) {
                    frameIndex++;
                    ImageIO.write(webcam.getImage(), ImageUtils.FORMAT_JPG, byteArrayOutputStream);
                    mkvDataAvailableCallback.onFrameDataAvailable(createKinesisVideoFrameFromFile(webcam, byteArrayOutputStream));
                }
                Thread.sleep(durationInMillis);
            } catch (final Exception e) {
                log.error("Frame interval wait interrupted by Exception ", e);
            }
        }
        webcam.close();
    }




    private KinesisVideoFrame createKinesisVideoFrameFromFile( Webcam fileName, ByteArrayOutputStream stream) {
        // fileName format: timecode-mediaType-isKeyFrame-frame, timecode is offset from beginning
        // 10000-audio-false-frame or 10999-video-true-frame
//        final String[] infos = fileName.split("-");
//        Preconditions.checkState(infos.length == INFO_LENGTH);
        final long startTime = System.currentTimeMillis();
        final long timestamp = startTime * HUNDREDS_OF_NANOS_IN_A_MILLISECOND
                - frameStartMillis * HUNDREDS_OF_NANOS_IN_A_MILLISECOND;

        final long trackId = VIDEO_TRACK_ID ;
        final int isKeyFrame = FRAME_FLAG_KEY_FRAME;
//        final Path path = Paths.get( "/" + fileName);
        try {
//            final byte[] bytes = Files.readAllBytes(stream.toByteArray());
            return new KinesisVideoFrame(frameIndex,
                    isKeyFrame,
                    timestamp,
                    timestamp,
                    FRAME_DURATION_0_MS * HUNDREDS_OF_NANOS_IN_A_MILLISECOND,
                    ByteBuffer.wrap(stream.toByteArray()),
                    trackId);
        } catch (Exception e) {
            log.error("Read file failed with Exception ", e);
        }

        return null;
    }

    private void stopFrameGenerator() {

    }
}
