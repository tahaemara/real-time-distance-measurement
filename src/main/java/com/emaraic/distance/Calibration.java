package com.emaraic.distance;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.Loader;
import static org.bytedeco.javacpp.helper.opencv_core.CV_RGB;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.CvContour;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import org.bytedeco.javacpp.opencv_imgproc;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_LIST;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_OTSU;
import static org.bytedeco.javacpp.opencv_imgproc.THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

/**
 *
 * @author Taha Emara 
 * Website: http://www.emaraic.com 
 * Email : taha@emaraic.com
 * Created on: Mar 10, 2018
 */
public class Calibration {

    private static final int DISTANCE_TO_OBJECT = 30;
    private static final double RADIUS_OF_MARKER = 1.0;
    private static final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

    public static boolean isPixelBlue(Mat image, int x, int y) {
        if (x < 0 || y < 0) {
            return false;
        }
        UByteIndexer srcIndexer = image.createIndexer();
        int[] hsv = new int[3];
        boolean blue = false;
        for (int i = 0; i < srcIndexer.rows(); i++) {
            for (int j = 0; j < srcIndexer.cols(); j++) {
                if (j == x && i == y) {
                    srcIndexer.get(i, j, hsv);
                    if (hsv[0] >= 100 && hsv[0] <= 130 && hsv[1] >= 50 && hsv[1] <= 255 && hsv[2] >= 50 && hsv[2] <= 255) {
                        blue = true;
                    }
                }
            }
        }
        return blue;
    }

    public static BufferedImage IplImageToBufferedImage(IplImage src) {
        Java2DFrameConverter paintConverter = new Java2DFrameConverter();
        Frame frame = converter.convert(src);
        return paintConverter.getBufferedImage(frame, 1);
    }

    public static void displayImage(IplImage imgage, String title) {
        BufferedImage img = IplImageToBufferedImage(imgage);
        JFrame frame = new JFrame();
        frame.setTitle(title);
        frame.setSize(new Dimension(410, 410));
        frame.setLocationRelativeTo(null);
        frame.setLayout(new FlowLayout());
        JLabel label = new JLabel();
        label.setIcon(new ImageIcon(img.getScaledInstance(400, 400, 400)));
        frame.add(label);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        try {
            FrameGrabber grabber = FrameGrabber.createDefault(0);
            grabber.start();
            Frame frame = grabber.grab();
            System.out.println("\nFrame size " + frame.imageWidth + " x " + frame.imageHeight);
            IplImage grabbedImage = converter.convert(frame);
            Mat img = converter.convertToMat(frame);
            displayImage(grabbedImage, "Grabbed Image");

            /*Resize Image (comment this section if you want to use default resolution)*/
            Size size = new Size(640, 640);
            resize(img, img, size);
            grabbedImage = new IplImage(img);

            /*Invert to HSV color space*/
            Mat imghsv = new Mat();
            cvtColor(img, imghsv, COLOR_BGR2HSV);

            /*Convert image to grayscale*/
            IplImage gray = cvCreateImage(cvGetSize(grabbedImage), IPL_DEPTH_8U, 1);
            cvCvtColor(grabbedImage, gray, CV_BGR2GRAY);

            /*Binarising Image*/
            IplImage binimg = cvCreateImage(cvGetSize(gray), IPL_DEPTH_8U, 1);
            cvThreshold(gray, binimg, 0, 255, THRESH_BINARY + CV_THRESH_OTSU);
            imwrite("binarise.jpg", new Mat(binimg));

            /*Find countour */
            CvMemStorage storage = cvCreateMemStorage(0);
            CvSeq contours = new CvSeq();
            cvFindContours(binimg, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE, cvPoint(0, 0));
            CvSeq ptr = new CvSeq();
            Mat m = new Mat(grabbedImage);
            for (ptr = contours; ptr != null; ptr = ptr.h_next()) {
                Point2f center = new Point2f();
                FloatPointer radius = new FloatPointer(1f);
                opencv_imgproc.minEnclosingCircle(new Mat(ptr), center, radius);

                if (isPixelBlue(imghsv, (int) center.x(), (int) center.y())) {
                    double area = opencv_imgproc.contourArea(new Mat(ptr), true);
                    if (area > 100) {
                        opencv_imgproc.cvCircle(grabbedImage, new CvPoint((int) center.x(), (int) center.y()), (int) radius.get(0),
                                CV_RGB(0, 255, 0), 3, 0, 0);
                        opencv_imgproc.putText(m, area + "", new Point((int) center.x(), (int) center.y()), 0, 2.0, new Scalar(0, 0, 0, 0));//print result above every digit
                        float rad = radius.get(0);
                        System.out.println("Radius " + rad);
                        double focallen = (DISTANCE_TO_OBJECT * rad) / RADIUS_OF_MARKER;
                        System.out.println("Focal lenght is " + focallen + " Pixels");
                    }
                }
            }//End for  countors

            /*Display output image to check that if marker (blue circle) is segmented correctly
             *If it is not segmented correcly try to tune area size condition (area>100)
             */
            displayImage(new IplImage(m), "Output Image");

        } catch (FrameGrabber.Exception ex) {
            ex.printStackTrace();
        }
    }
}
