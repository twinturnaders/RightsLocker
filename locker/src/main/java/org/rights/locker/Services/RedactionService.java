package org.rights.locker.Services;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

@Component
public class RedactionService {
    static { nu.pattern.OpenCV.loadLocally(); } // loads bundled native for your OS

    public BufferedImage blurFaces(BufferedImage frame, CascadeClassifier classifier) {
        Mat mat = bufferedImageToMat(frame); // write small util to convert
        MatOfRect faces = new MatOfRect();
        classifier.detectMultiScale(mat, faces, 1.1, 3, 0, new Size(24,24), new Size());

        for (Rect r : faces.toArray()) {
            Mat roi = mat.submat(r);
            Imgproc.GaussianBlur(roi, roi, new Size(45,45), 30);
        }
        return matToBufferedImage(mat);
    }

    public void redactVideo(Path inMp4, Path outMp4, Path cascadePath) throws Exception {
        CascadeClassifier face = new CascadeClassifier(cascadePath.toString());
        // approach A (simplest): extract frames → blur → re-encode with ffmpeg images → mp4
        // approach B (faster): use ffmpeg filtergraph with a detection mask (advanced later)
    }
}