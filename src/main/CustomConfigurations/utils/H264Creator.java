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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class H264Creator implements Runnable {
    private static final String PATH = "C:\\Users\\Public\\Videos";
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss");
    private static final String videoFormat = "mp4";
    private static final int COUNT_FRAME = 100;
    private Webcam webcam;

    @Override
    public void run() {
        webcam = Webcam.getDefault();
        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.open();

        LocalDateTime now = LocalDateTime.now();
        File saveVideo = new File(PATH, String.format("%s - %s.%s", now.format(format), webcam.getName(), videoFormat));

        IMediaWriter writer = ToolFactory.makeWriter(saveVideo.getAbsolutePath());
        Dimension size = webcam.getViewSize();
        writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MJPEG, size.width, size.height);
        long start = System.currentTimeMillis();

        for (int i = 0; i < COUNT_FRAME; i++) {
            System.out.println(i);
            BufferedImage image = ConverterFactory.convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
            IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

            IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis() - start) * 1000);
            frame.setKeyFrame(i == 0);
            frame.setQuality(0);

            writer.encodeVideo(0, frame);

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        writer.close();
        System.out.println("Video recorded in file: " + saveVideo.getAbsolutePath());
    }

    public static void main(String[] args) {
        new Thread(new H264Creator()).start();
    }
}