package com.scitequest.martin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.Test;

public class ImageImplTest {

    private void assertImageEquals(Image a, Image b) {
        DataBuffer bufA = a.getWrapped().getRaster().getDataBuffer();
        DataBuffer bufB = b.getWrapped().getRaster().getDataBuffer();
        assertEquals(bufA.getSize(), bufB.getSize());
        for (int i = 0; i < bufA.getSize(); i++) {
            assertEquals(bufA.getElemDouble(i), bufB.getElemDouble(i), 0.0);
        }
    }

    private void assertImageNotEquals(Image a, Image b) {
        DataBuffer bufA = a.getWrapped().getRaster().getDataBuffer();
        DataBuffer bufB = b.getWrapped().getRaster().getDataBuffer();
        if (bufA.getSize() != bufB.getSize()) {
            return;
        }
        for (int i = 0; i < bufA.getSize(); i++) {
            if (bufA.getElemDouble(i) != bufB.getElemDouble(i)) {
                return;
            }
        }
        assertTrue(false);
    }

    @Test
    public void duplicateChecker() throws IOException {
        String imagePath = "src/test/resources/img/SYNI - SM67 - 60sec - A - 2.tif";
        Image img = ImageImpl.read(Path.of(imagePath));
        Image imgDup = img.duplicate();

        assertImageEquals(img, imgDup);

        int[] pixels = new int[1];
        pixels[0] = 42;
        img.getWrapped().getRaster().setPixel(0, 0, pixels);

        assertImageNotEquals(img, imgDup);
        imgDup.getWrapped().getRaster().getPixel(0, 0, pixels);
        int value = pixels[0];
        assertNotEquals(42, value);
    }

    @Test
    public void invertChecker() throws IOException {
        String imagePath = "src/test/resources/img/SYNI - SM67 - 60sec - A - 2.tif";
        Image img = ImageImpl.read(Path.of(imagePath));
        Image imgDup = ImageImpl.read(Path.of(imagePath));

        imgDup.invert();
        assertImageNotEquals(img, imgDup);

        imgDup.invert();
        assertImageEquals(img, imgDup);
    }

    @Test
    public void subtractChecker() throws IOException {
        String imagePath = "src/test/resources/img/checker.tif";
        Image img = ImageImpl.read(Path.of(imagePath));
        Image imgDup = ImageImpl.read(Path.of(imagePath));

        img.subtract(3);

        DataBuffer buf = img.getWrapped().getRaster().getDataBuffer();
        DataBuffer bufDup = imgDup.getWrapped().getRaster().getDataBuffer();
        for (int i = 0; i < buf.getSize(); i++) {
            assertEquals(bufDup.getElem(i) - 3, buf.getElem(i));
        }

        img.subtract(Integer.MAX_VALUE);
        for (int i = 0; i < buf.getSize(); i++) {
            assertEquals(0, buf.getElem(i));
        }
    }
}
