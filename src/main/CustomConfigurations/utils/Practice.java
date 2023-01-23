package utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Practice {

    public static void main(String[] args) throws InterruptedException{
        try {

            Webcam w = Webcam.getDefault();
            w.setViewSize(WebcamResolution.VGA.getSize());

            ServerSocket ss=new ServerSocket(6666);
            Socket socket = ss.accept();
            InputStream inputStreamReader = socket.getInputStream();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
