package utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import org.bytedeco.javacv.FrameGrabber;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public class H264Creator implements Runnable{
    private static final String PATH = "src/main/resources/data/customh264";
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss");
    private static final String videoFormat = "mkv";
    private static final int COUNT_FRAME = 375;
    private static Webcam webcam;

    static {
        webcam = Webcam.getDefault();
        webcam.setAutoOpenMode(true);
        webcam.setViewSize(WebcamResolution.VGA.getSize());
    }
    @Override
    public void run() {
        File saveVideo = new File(PATH, String.format("%s.%s", "frame",videoFormat));

        IMediaWriter writer = ToolFactory.makeWriter(saveVideo.getAbsolutePath());
        Dimension size = webcam.getViewSize();
        writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, size.width, size.height);
        long start = System.currentTimeMillis();

        for (int i = 0; i < COUNT_FRAME; i++) {
            BufferedImage image = ConverterFactory.convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
            IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

            IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis() - start) * 1000);
            frame.setKeyFrame(i == 0);
            frame.setQuality(0);
            writer.encodeVideo(0, frame);
        }
        writer.close();
    }

    public InputStream getInputStreamOfMkvFile() throws IOException {

        File saveVideo = new File(PATH, String.format("%s.%s", "frame",videoFormat));

        IMediaWriter writer = ToolFactory.makeWriter(saveVideo.getAbsolutePath());
        Dimension size = webcam.getViewSize();
        writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, size.width, size.height);
        long start = System.currentTimeMillis();

        for (int i = 0; i < COUNT_FRAME; i++) {
            BufferedImage image = ConverterFactory.convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
            IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

            IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis() - start) * 1000);
            frame.setKeyFrame(i == 0);
            frame.setQuality(0);
            writer.encodeVideo(0, frame);
        }
        writer.close();
        return Files.newInputStream(Paths.get(PATH+"/frame.mkv"));
//        return null;
    }
    public static ByteBuffer getImageByteBuffer(Webcam webcam) throws FrameGrabber.Exception {

        BufferedImage image = ConverterFactory.convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
        IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

        IVideoPicture frame = converter.toPicture(image, 0 * 1000);
        frame.setKeyFrame(true);
        frame.setQuality(0);
        return frame.getByteBuffer();
    }

    public static void main(String[] args) {
//        new Thread(new H264Creator()).start();

        try {
            H264Creator creator = new H264Creator();
             creator.getInputStreamOfMkvFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


//    final Java2DFrameConverter converter = new Java2DFrameConverter();
//
//    // Show drone camera
//    FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(webcam.getName());
//
//        grabber.setFrameRate(25);
//        grabber.setFormat(".h264");
//        grabber.setVideoBitrate(25000000);
//        grabber.setVideoOption("preset", "ultrafast");
//        grabber.setNumBuffers(0);
//
//        grabber.start();
//
//    // Grab frames as long as the thread is running
//        while(true){
//        final Frame frame = grabber.grab();
//        if (frame != null) {
//            final BufferedImage bufferedImage = converter.convert(frame);
//            if (bufferedImage != null) {
//                _cameraView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
//            }
//        }
//        Thread.sleep( 1000 / _frameRate );// don't grab frames faster than they are provided
//        grabber.flush();
//    }
//        _grabber.close();
}