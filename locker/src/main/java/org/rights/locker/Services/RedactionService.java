package org.rights.locker.Services;

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.bytedeco.opencv.global.opencv_imgproc.GaussianBlur;

@Service
public class RedactionService {

  private final Java2DFrameConverter j2d = new Java2DFrameConverter();
  private final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();

  /**
   * Blur faces in a single frame using a Haar/LBP cascade.
   */
  public BufferedImage blurFaces(BufferedImage frame, CascadeClassifier classifier) {
    // BufferedImage -> Mat
    Mat mat = toMat.convertToMat(j2d.convert(frame));

    // Detect
    RectVector faces = new RectVector();
    // scaleFactor=1.1, minNeighbors=3, flags=0, minSize=(24,24), maxSize=(0,0=auto)
    classifier.detectMultiScale(mat, faces, 1.1, 3, 0, new Size(24, 24), new Size());

    // Blur each region
    for (long i = 0; i < faces.size(); i++) {
      Rect r = faces.get(i);
      Mat roi = new Mat(mat, r);
      GaussianBlur(roi, roi, new Size(45, 45), 30); // tweak kernel/sigma as needed
    }

    // Mat -> BufferedImage
    return j2d.convert(toMat.convert(mat));
  }

  /**
   * Skeleton for video redaction (frame-by-frame).
   */
  public void redactVideo(Path inMp4, Path outMp4, Path cascadePath) throws Exception {
    CascadeClassifier face = new CascadeClassifier(cascadePath.toString());
    if (face.empty()) throw new IllegalArgumentException("Cascade not found: " + cascadePath);

    // OPTION A: Use JavaCV grabber/recorder (no external ffmpeg CLI needed)
    try (var grabber = new org.bytedeco.javacv.FFmpegFrameGrabber(inMp4.toString());
         var recorder = new org.bytedeco.javacv.FFmpegFrameRecorder(outMp4.toString(),
                 grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels())) {

      grabber.start();

      // Mirror input encoding params where sensible
      recorder.setFrameRate(grabber.getFrameRate());
      recorder.setVideoCodec(grabber.getVideoCodec());  // or set to avcodec.AV_CODEC_ID_H264
      recorder.setFormat("mp4");
      recorder.setAudioCodec(grabber.getAudioCodec());
      recorder.start();

      var toMat = new OpenCVFrameConverter.ToMat();
      var j2d = new Java2DFrameConverter();

      org.bytedeco.javacv.Frame f;
      while ((f = grabber.grab()) != null) {
        if (f.image != null) {
          // Convert to BufferedImage -> blur -> back to Frame
          BufferedImage bi = j2d.convert(f);
          BufferedImage redacted = blurFaces(bi, face);
          org.bytedeco.javacv.Frame out = j2d.convert(redacted);
          recorder.record(out);
        } else if (f.samples != null) {
          // pass-through audio frames unchanged
          recorder.record(f);
        }
      }

      recorder.stop();
      grabber.stop();
    }
  }
}