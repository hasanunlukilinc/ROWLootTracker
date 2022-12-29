package io.geniusbrain;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

/**
 * @author Hasan Ünlü KILINÇ
 */
@SuppressWarnings("SpellCheckingInspection")
public class LootReader {

    Logger logger = Logger.getLogger(LootReader.class.getName());
    private ITesseract tesseract = null;
    private String lastRead = null;
    private Rectangle capturedRectangle = null;

    private Robot robot = null;

    private String LOG_PATH;
    String desktopPath = null;

    public void start() throws IOException, AWTException {

        //Kullanıcının masaüstü konumu (loglar için lazım olacak)
        desktopPath = (new File(System.getProperty("user.home"), "Desktop")).getAbsolutePath();


        configureLogging();
        logger.info("Logging is configured!");


        configureDisplayResoulation();


        //OCR Ayarları
        configureOCR();
        logger.info("OCR Engine is activated");

        //Ekran Yakalayıcı Aktif Edildi
        robot = new Robot();

        //Sanal Thread
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::readLoot, 0, 200, TimeUnit.MILLISECONDS);
        logger.info("Ready to Go!");
        logger.info("Respect from SkyDragon!");
    }

    private void configureOCR() {
        tesseract = new Tesseract();
        tesseract.setLanguage("eng");
        //Tesseract kütüphanesinin  dosya konumunu dinamik olarak ayarlıyoruz
        File tessDataFolder = LoadLibs.extractTessResources("tessdata");
        tesseract.setDatapath(tessDataFolder.getAbsolutePath());
    }

    private void configureLogging() throws IOException {
        String LOG_FOLDER = "ROWLootTrackerLogs";
        Path logFolder = Paths.get(desktopPath + "\\" + LOG_FOLDER);
        if (!Files.exists(logFolder)) {
            Files.createDirectories(logFolder);
        }
        LOG_PATH = logFolder.toString();
        FileHandler fileHandler = new FileHandler(desktopPath + "\\" + LOG_FOLDER + "\\" + LOG_FOLDER + System.currentTimeMillis() + ".log");
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
    }

    private void configureDisplayResoulation() {
        Rectangle screenRectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        int xCoordinate = (int) (screenRectangle.getWidth() * 0.70); // X kordinatı
        int yCoordinate = (int) (screenRectangle.getHeight() * 0.50); // Y kordinatı
        int croppedImageWidth = (int) (screenRectangle.getWidth() - xCoordinate);
        int croppedImageHeight = (int) (screenRectangle.getHeight() - yCoordinate);
        capturedRectangle = new Rectangle(xCoordinate, yCoordinate, croppedImageWidth, croppedImageHeight);
        logger.info("Screen Resolution is:  " + (int) screenRectangle.getWidth() + "x" + (int) screenRectangle.getHeight());
    }

    private void readLoot() {
        try {
            long now = System.currentTimeMillis();
            final BufferedImage croppedImage = takeScreenShootAndCrop();

            String text = readLootInfo(croppedImage);
            ImageIO.write(croppedImage, "png", new File(LOG_PATH + "\\currentPicture" + ".png"));
            //Eğer akış yok ise ve taranan değer aynı ise okuma yapmana gerek yok!
            if (lastRead == null || !lastRead.equals(text)) {
                if (!text.isBlank()) {
                    lastRead = text;
                    checkItems(text, croppedImage, now);
                }
            }

        } catch (IOException | TesseractException e) {
            e.printStackTrace();
        }
    }

    private void checkItems(String text, BufferedImage ss, long now) throws IOException {
        if (Stream.of("War Scythe", "War ", " Scythe").anyMatch(text::contains)) {
            logger.warning("War Scythe KITLANDI!");
            saveImage(ss, "War Scythe", now);
        } else if (text.contains("Epic Upgrade Scroll") || text.contains("Epic Upgrade")) {
            logger.warning("Epic Upgrade Scroll KITLANDI!");
            saveImage(ss, "Epic Upgrade Scroll", now);
        } else if (text.contains("Eternal Axe")) {
            logger.warning("Eternal Axe KITLANDI!");
            saveImage(ss, "Eternal Axe", now);
        } else if (Stream.of("Hatred's Blade", "Hatreds Blade", "Hatred s Blade", "Hatred sBlade").anyMatch(text::contains)) {
            logger.warning("Hatred's Blade KITLANDI!");
            saveImage(ss, "Hatred's Blade", now);
        } else if (text.contains("Imperial")) {
            logger.warning("Imperial KITLANDI!");
            saveImage(ss, "Imperial", now);
        } else if (Stream.of("Golden Snake", "Snake").anyMatch(text::contains)) {
            logger.warning("Golden Snake KITLANDI!");
            saveImage(ss, "Golden Snake", now);
        } else if (text.contains("gained") | text.contains("kazanıldı")) {
            logger.warning("Mob Drop!");
        }
    }

    private String readLootInfo(BufferedImage image) throws TesseractException {
        return tesseract.doOCR(image);
    }

    private BufferedImage takeScreenShootAndCrop() throws IOException {
        //Ekranın tamamını işlememek için sadece drop akışının olduğu alanı kesiyoruz
        //Kesimin Başlayacağı kordinatlar
        return robot.createScreenCapture(capturedRectangle);

    }

    private void saveImage(BufferedImage image, String type, long now) throws IOException {
        ImageIO.write(image, "png", new File(LOG_PATH + "\\" + type + "_" + now + ".png"));

    }

}
