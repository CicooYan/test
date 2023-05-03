package tool;

import model.IndexNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public enum ProcessTool {
    INSTANCE;

    /**
     * Create mock index hierarchy nodes for test use
     *
     * @return
     */
    public List<IndexNode> getMockIndexNodes(ArrayList<Integer[]> data) {
        List<IndexNode> nodes = new ArrayList<>();
        int sencecount = 1;
        int shotcount = 1;
        int subshotcount = 1;

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i)[0] == 1) {
                IndexNode scene = new IndexNode("Scene" + Integer.toString(sencecount), data.get(i)[1] / 30.0);
                shotcount = 1;
                IndexNode shot = new IndexNode("Shot" + Integer.toString(shotcount), data.get(i)[1] / 30.0);
                sencecount++;
                shotcount++;
                scene.addChildren(shot);
                nodes.add(scene);
            } else if (data.get(i)[0] == 2) {
                IndexNode shot = new IndexNode("Shot" + Integer.toString(shotcount), data.get(i)[1] / 30.0);
                nodes.get(sencecount - 2).addChildren(shot);
                shotcount++;
                subshotcount = 1;
            } else if (data.get(i)[0] == 3) {
                IndexNode shot = nodes.get(sencecount - 2).getChildren().get(shotcount - 2);
                if (subshotcount == 1) {
                    IndexNode first = new IndexNode("Subshot" + Integer.toString(subshotcount), shot.getTime());
                    shot.addChildren(first);
                    subshotcount++;
                }
                IndexNode subshot = new IndexNode("Subshot" + Integer.toString(subshotcount), data.get(i)[1] / 30.0);
                shot.addChildren(subshot);
                subshotcount++;

            }
        }
        for (IndexNode node : nodes) {
            System.out.println(node.getName() + " " + node.getTime() * 30);
            for (IndexNode children : node.getChildren()) {
                System.out.println(children.getName() + " " + children.getTime() * 30);
                for (IndexNode subchild : children.getChildren()) {
                    System.out.println(subchild.getName() + " " + subchild.getTime() * 30);
                }
            }
        }

        return nodes;
    }

    public List<IndexNode> processfile(String rgbUrl) {
        ArrayList<Integer[]> data = new ArrayList<>();
        data.add(new Integer[]{1, 0});
        System.out.println(rgbUrl);
        File selectedFile = new File(rgbUrl);
        int width = 480; // width of the video frames
        int height = 270; // height of the video frames
        int fps = 30; // frames per second of the video


        // create an array to store all frames

        ArrayList<int[][][]> histograms1 = new ArrayList<>();
        ArrayList<Double> cummulativeDiffs1 = new ArrayList<>();
        ArrayList<Double> globalDiffs1 = new ArrayList<>();
        // create an array to store histograms


        // read the video file and store each frame
        try {
            RandomAccessFile raf = new RandomAccessFile(selectedFile, "r");
            FileChannel channel = raf.getChannel();

            ByteBuffer buffer = ByteBuffer.allocate(width * height * 3);
            BufferedImage image1 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int i = 0;
            while (true) {
                buffer.clear();
                int eof = channel.read(buffer);
                buffer.rewind();
                if (eof == -1) {
                    break;
                }
                double cummulativeDiff = 0;
                double globalDiff = 0;
                int[][][] hist = new int[4][4][4];
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int r = buffer.get() & 0xff;
                        int g = buffer.get() & 0xff;
                        int b = buffer.get() & 0xff;
                        int rgb = (r << 16) | (g << 8) | b;
                        if (i == 0) {
                            image1.setRGB(x, y, rgb);
                            i = 1;
                        } else {
                            int rgb1 = image1.getRGB(x, y);
                            int r1 = (rgb1 >> 16) & 0xFF;
                            int g1 = (rgb1 >> 8) & 0xFF;
                            int b1 = (rgb1) & 0xFF;
                            cummulativeDiff += Math.abs(r - r1) + Math.abs(g - g1)
                                    + Math.abs(b - b1);
                            globalDiff += r1 - r + g1 - g + b1 - b;
                            image1.setRGB(x, y, rgb);
                        }
                        hist[r / 64][g / 64][b / 64]++;
                    }
                }
                histograms1.add(hist);
                if (i > 0) {
                    cummulativeDiff = cummulativeDiff / (double) (width * height);
                    globalDiff = Math.abs(globalDiff) / (double) (width * height);
                    cummulativeDiffs1.add(cummulativeDiff);
                    globalDiffs1.add(globalDiff);
                }
                System.out.println(histograms1.size());


            }
            channel.close();
            raf.close();
        } catch (IOException er) {
            er.printStackTrace();
        }

        System.out.println("!");

        ArrayList<Double> test = new ArrayList<>();
        test.add(1.1);
        test.add(1.2);
        System.out.println(test.size());


        // extract shots from video using Histogram
        double[] histogramDiffs = new double[histograms1.size() - 1];
        System.out.println(histograms1.size());
        for (int i = 0; i < histograms1.size() - 1; i++) {
            double histogramDiff = 0;
            System.out.println(i);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    for (int r = 0; r < 4; r++) {
                        for (int g = 0; g < 4; g++) {
                            for (int b = 0; b < 4; b++) {
                                histogramDiff += Math.abs(histograms1.get(i)[r][g][b] - histograms1.get(i + 1)[r][g][b]);
                            }
                        }
                    }
                }
            }
            histogramDiff = histogramDiff / (double) (width * height * 3);
            //   System.out.print(i);
            //  System.out.print("\t");
            //  System.out.println(histogramDiff);
            histogramDiffs[i] = histogramDiff;
        }
        for (int i = 0; i < histograms1.size() - 1; i++) {
            System.out.print(i);
            System.out.print("\t");
            System.out.println(cummulativeDiffs1.get(i));
            System.out.print("\t");
            System.out.println(histogramDiffs[i]);
        }

        double[] histobuffer = new double[histograms1.size() - 1];
        double[] cumubuffer = new double[histograms1.size() - 1];

        for (int i = 5; i < histograms1.size() - 1; i++) {
            double histrocount = 0;
            histrocount = histogramDiffs[i] + histogramDiffs[i - 4] + histogramDiffs[i - 3] + histogramDiffs[i - 2] + histogramDiffs[i - 1];
            histobuffer[i] = histrocount / 5;
            System.out.println(histobuffer[i]);
            double cumucount = 0;
            cumucount = cummulativeDiffs1.get(i - 1) + cummulativeDiffs1.get(i - 1) + cummulativeDiffs1.get(i - 1) + cummulativeDiffs1.get(i - 1) + cummulativeDiffs1.get(i - 1);
            cumubuffer[i] = cumucount / 5;
            System.out.println(cumubuffer[i]);
        }

        try {
            RandomAccessFile raf = new RandomAccessFile(selectedFile, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(width * height * 3);
            BufferedImage image1 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage image2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int count = 0;
            int sencecount = 0;
            int subshotflag = 0;
            int twoframesecne = 0;
            int twoframeshot = 0;
            for (int i = 0; i < histograms1.size(); i++) {
                System.out.println(i);
                buffer.clear();
                channel.read(buffer);
                buffer.rewind();
                count++;
                sencecount++;
                if(i>2){
                    image2=image1;
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int r = buffer.get() & 0xff;
                            int g = buffer.get() & 0xff;
                            int b = buffer.get() & 0xff;
                            int rgb = (r << 16) | (g << 8) | b;
                            image1.setRGB(x, y, rgb);
                        }
                    }
                }
                if (twoframesecne == 1 || twoframeshot == 1) {
                    count = 0;


                    if (subshotflag == 1) {
                        subshotflag = 0;
                    }
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int r = buffer.get() & 0xff;
                            int g = buffer.get() & 0xff;
                            int b = buffer.get() & 0xff;
                            int rgb = (r << 16) | (g << 8) | b;
                            image.setRGB(x, y, rgb);
                        }
                    }
                    if (twoframesecne == 1) {
                        sencecount = 0;
                        File outputfile2 = new File(i-2 + "scenebefore2" + ".jpg");

                        ImageIO.write(image2, "jpg", outputfile2);
                        File outputfile1 = new File(i-1 + "scenebefore1" + ".jpg");

                        ImageIO.write(image1, "jpg", outputfile1);
                        File outputfile = new File(i + "scene" + ".jpg");

                        ImageIO.write(image, "jpg", outputfile);
                        data.add(new Integer[]{1, i});
                        ;
                    } else {
                        File outputfile2 = new File(i-2 + "scenebefore2" + ".jpg");

                        ImageIO.write(image2, "jpg", outputfile2);
                        File outputfile1 = new File(i-1 + "scenebefore1" + ".jpg");

                        ImageIO.write(image1, "jpg", outputfile1);
                        File outputfile = new File(i + "shot" + ".jpg");

                        ImageIO.write(image, "jpg", outputfile);
                        data.add(new Integer[]{2, i});
                        ;
                    }
                    twoframeshot = 0;
                    twoframesecne = 0;

                } else if (i > 0 && sencecount > 35 && count > 5 && (cummulativeDiffs1.get(i) > 150 || (histogramDiffs[i - 1] > 27000 && cummulativeDiffs1.get(i) > 55) || (histogramDiffs[i - 1] > 11000 && cummulativeDiffs1.get(i) > 128) || (histogramDiffs[i - 1] > 19000 && cummulativeDiffs1.get(i) > 75) || (count > 20 && i > 10 && cumubuffer[i - 6] == 0 && cumubuffer[i - 1] > 3)
                )) {
                    count = 0;
                    sencecount = 0;

                    if (subshotflag == 1) {
                        subshotflag = 0;
                    }

                    data.add(new Integer[]{1, i});
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int r = buffer.get() & 0xff;
                            int g = buffer.get() & 0xff;
                            int b = buffer.get() & 0xff;
                            int rgb = (r << 16) | (g << 8) | b;
                            image.setRGB(x, y, rgb);
                        }
                    }
                    File outputfile2 = new File(i-2 + "scenebefore2" + ".jpg");

                    ImageIO.write(image2, "jpg", outputfile2);
                    File outputfile1 = new File(i-1 + "scenebefore1" + ".jpg");

                    ImageIO.write(image1, "jpg", outputfile1);
                    File outputfile = new File(i + "scene" + ".jpg");
                    ImageIO.write(image, "jpg", outputfile);

                } else if (i < histograms1.size() - 1 && i > 0 && sencecount > 35 && count > 5 && (cummulativeDiffs1.get(i + 1) > 150 || i < histograms1.size() - 1 && histogramDiffs[i - 1] + histogramDiffs[i] > 29000 && cummulativeDiffs1.get(i) + cummulativeDiffs1.get(i + 1) > 80)) {
                    twoframesecne = 1;
                    continue;
                } else if (i > 0 && count > 15 && (cumubuffer[i - 1] > 57 && histogramDiffs[i - 1] > 14000 || (histogramDiffs[i - 1] > 10000 && cummulativeDiffs1.get(i) > 55) ||
                        (histogramDiffs[i - 1] > 4000 && cummulativeDiffs1.get(i) > 60 && histogramDiffs[i - 1] < 12000) ||
                        (histogramDiffs[i - 1] > 6500 && cummulativeDiffs1.get(i) > 53 && histogramDiffs[i - 1] < 12000)
                )) {
                    count = 0;
                    if (subshotflag == 1) {
                        subshotflag = 0;
                    }

                    data.add(new Integer[]{2, i});
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int r = buffer.get() & 0xff;
                            int g = buffer.get() & 0xff;
                            int b = buffer.get() & 0xff;
                            int rgb = (r << 16) | (g << 8) | b;
                            image.setRGB(x, y, rgb);
                        }
                    }
                    File outputfile2 = new File(i-2 + "scenebefore2" + ".jpg");

                    ImageIO.write(image2, "jpg", outputfile2);
                    File outputfile1 = new File(i-1 + "scenebefore1" + ".jpg");

                    ImageIO.write(image1, "jpg", outputfile1);
                    File outputfile = new File(i + "shot" + ".jpg");
                    ImageIO.write(image, "jpg", outputfile);

                } else if (i > 0 && count > 15 && (i < histograms1.size() - 1 && histogramDiffs[i - 1] + histogramDiffs[i] > 10000 && cummulativeDiffs1.get(i) + cummulativeDiffs1.get(i + 1) > 80) && !(histogramDiffs[i] > 19000 && cummulativeDiffs1.get(i + 1) > 70)) {
                    twoframeshot = 1;
                    continue;
                } else if (i > 0 && ((5000 < histogramDiffs[i - 1] && cummulativeDiffs1.get(i) > 47) || (3200 < histogramDiffs[i - 1] && cummulativeDiffs1.get(i) > 38)) && count > 30 && subshotflag == 0) {
                    count = 0;

                    subshotflag = 1;

                    data.add(new Integer[]{3, i});
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int r = buffer.get() & 0xff;
                            int g = buffer.get() & 0xff;
                            int b = buffer.get() & 0xff;
                            int rgb = (r << 16) | (g << 8) | b;
                            image.setRGB(x, y, rgb);
                        }
                    }
                    File outputfile2 = new File(i-2 + "scenebefore2" + ".jpg");

                    ImageIO.write(image2, "jpg", outputfile2);
                    File outputfile1 = new File(i-1 + "scenebefore1" + ".jpg");

                    ImageIO.write(image1, "jpg", outputfile1);
                    File outputfile = new File(i + "subshot" + ".jpg");
                    ImageIO.write(image, "jpg", outputfile);

                } else if (count > 30 && subshotflag == 1) {
                    if (histobuffer[i - 1] < 20) {


                        data.add(new Integer[]{3, i});
                        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                int r = buffer.get() & 0xff;
                                int g = buffer.get() & 0xff;
                                int b = buffer.get() & 0xff;
                                int rgb = (r << 16) | (g << 8) | b;
                                image.setRGB(x, y, rgb);
                            }
                        }
                        File outputfile2 = new File(i-2 + "scenebefore2" + ".jpg");

                        ImageIO.write(image2, "jpg", outputfile2);
                        File outputfile1 = new File(i-1 + "scenebefore1" + ".jpg");

                        ImageIO.write(image1, "jpg", outputfile1);
                        File outputfile = new File(i + "subshot" + ".jpg");
                        ImageIO.write(image, "jpg", outputfile);

                        subshotflag = 0;
                    }
                }
            }
            channel.close();
            raf.close();
        } catch (IOException er) {
            er.printStackTrace();
        }

        return  getMockIndexNodes(data);
    }
}
