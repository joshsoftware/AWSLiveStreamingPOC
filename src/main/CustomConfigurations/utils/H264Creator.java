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
import org.joda.time.DateTime;

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
    private static final int COUNT_FRAME = 25;
    private static Webcam webcam;

    public volatile String writeFile;

    static {
        webcam = Webcam.getDefault();
        webcam.setAutoOpenMode(true);
        webcam.setViewSize(WebcamResolution.VGA.getSize());
    }

    public H264Creator(String fileName) {
        this.writeFile = fileName;
    }

    public H264Creator() {
    }

    @Override
    public void run() {
        File saveVideo = new File(PATH, writeFile);

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
        System.out.println("start time:{}" + DateTime.now());
        File saveVideo = new File(PATH, writeFile);

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
        System.out.println("end time:{}" + DateTime.now());
        return Files.newInputStream(Paths.get(PATH+"/"+writeFile));
    }
    public static ByteBuffer getImageByteBuffer(Webcam webcam) throws FrameGrabber.Exception {

        BufferedImage image = ConverterFactory.convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
        IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

        IVideoPicture frame = converter.toPicture(image, 0 * 1000);
        frame.setKeyFrame(true);
        frame.setQuality(0);
        return frame.getByteBuffer();
    }

}