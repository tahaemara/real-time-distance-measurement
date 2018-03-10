package com.emaraic.distance;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.opencv_core.CvContour;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Scalar;
import static org.bytedeco.javacpp.helper.opencv_core.CV_RGB;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import org.bytedeco.javacpp.opencv_core.Size;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import org.bytedeco.javacpp.opencv_imgproc;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_LIST;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_OTSU;
import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.cvCircle;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.minEnclosingCircle;
import static org.bytedeco.javacpp.opencv_imgproc.putText;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

/**
 *
 * @author Taha Emara Website: http://www.emaraic.com Email : taha@emaraic.com
 * Created on: Mar 10, 2018
 */
public class DistanceMeasurement {

    private static final double FOCAL_LENGTH = 906.107;//focal length in pixels obtained from Calibration class
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

    public static void main(String[] args) {
        try {
            OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
            grabber.start();
            CanvasFrame canvas = new CanvasFrame("Distance Measuremtn - Emaraic");

            canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            canvas.setCanvasSize(660, 660);
            canvas.setLocationRelativeTo(null);
            while (true) {
                Frame frame = grabber.grab();
                IplImage grabbedImage = converter.convert(frame);

                if (grabbedImage != null && canvas.isVisible()) {
                    Mat img = converter.convertToMat(frame);

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
                    cvThreshold(gray, binimg, 0, 255, CV_THRESH_OTSU);

                    /*Find countour */
                    CvMemStorage storage = cvCreateMemStorage(0);
                    CvSeq contours = new CvSeq();
                    cvFindContours(binimg, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE, cvPoint(0, 0));
                    CvSeq ptr = new CvSeq();
                    Mat m = new Mat(grabbedImage);
                    for (ptr = contours; ptr != null; ptr = ptr.h_next()) {
                        /*Find Enclosing Circles*/
                        Point2f center = new Point2f();
                        FloatPointer radius = new FloatPointer(1f);
                        opencv_imgproc.minEnclosingCircle(new Mat(ptr), center, radius);
                        /*Check, if the color of the center picel is blue, it is our marker*/
                        if (isPixelBlue(imghsv, (int) center.x(), (int) center.y())) {
                            double area = contourArea(new Mat(ptr), true);
                            if (area > 100) {
                                cvCircle(grabbedImage, new CvPoint((int) center.x(), (int) center.y()), (int) radius.get(0),
                                        CV_RGB(0, 255, 0), 3, 0, 0);
                                float rad = radius.get(0);
                                System.out.println("Radius is " + rad);
                                double distance = (FOCAL_LENGTH * RADIUS_OF_MARKER) / rad;
                                System.out.println("Distance in cm " + distance);
                                putText(m, "Distance is : " + distance + " cm", new Point(200, 100), 0, 0.5, new Scalar(0, 255, 0, 0));
                            }
                        }
                    }//End for  countors
                    canvas.showImage(converter.convert(m));
                    Thread.sleep(150);
                }//End if
            }//End while
        } catch (FrameGrabber.Exception | InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
