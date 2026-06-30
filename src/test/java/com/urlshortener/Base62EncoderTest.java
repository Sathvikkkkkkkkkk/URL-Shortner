package com.urlshortener;

import com.urlshortener.service.Base62Encoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void testEncodeLength() {
        String originalUrl = "https://www.google.com";
        long timestamp = System.currentTimeMillis();
        
        String shortCode = Base62Encoder.encode(originalUrl, timestamp, 0);
        
        assertNotNull(shortCode);
        assertEquals(6, shortCode.length());
    }

    @Test
    void testCharsetValidity() {
        String originalUrl = "https://www.wikipedia.org/wiki/Main_Page";
        long timestamp = System.currentTimeMillis();
        
        String shortCode = Base62Encoder.encode(originalUrl, timestamp, 0);
        
        // Match regex for alphanumeric values
        assertTrue(shortCode.matches("^[a-zA-Z0-9]{6}$"), "Short code should be 6 alphanumeric characters");
    }

    @Test
    void testSaltResolutionDiffers() {
        String originalUrl = "https://github.com";
        long timestamp = System.currentTimeMillis();
        
        String shortCode1 = Base62Encoder.encode(originalUrl, timestamp, 0);
        String shortCode2 = Base62Encoder.encode(originalUrl, timestamp, 1);
        String shortCode3 = Base62Encoder.encode(originalUrl, timestamp, 2);
        
        assertNotEquals(shortCode1, shortCode2);
        assertNotEquals(shortCode2, shortCode3);
        assertNotEquals(shortCode1, shortCode3);
    }
}
