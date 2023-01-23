package utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.util.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WebcamUtils {

    public static byte[] getImageByteArrayFromWebCam(Webcam webcam){
        webcam.open();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(webcam.getImage(), ImageUtils.FORMAT_JPG, baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        webcam.close();

        return baos.toByteArray();
    }

    public static void webcamStreaming(){
        Webcam.setAutoOpenMode(true);
        Webcam webcam = Webcam.getDefault();
        Dimension dimension = new Dimension(320, 240);
        webcam.setViewSize(dimension);

//        StreamServerAgent serverAgent = new StreamServerAgent(webcam, dimension);
//        serverAgent.start(new InetSocketAddress("localhost", 0));
    }
}
