package org.alquds.library;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class PdfFixtures {
    static byte[] twoPages() throws IOException {
        try (PdfDocument document = new PdfDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int index = 0; index < 2; index++) {
                PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(400, 600, index + 1).create());
                page.getCanvas().drawColor(Color.WHITE);
                Paint ink = new Paint(); ink.setColor(Color.BLACK); ink.setTextSize(24);
                page.getCanvas().drawText("Local PDF test - page " + (index + 1), 25, 60, ink);
                document.finishPage(page);
            }
            document.writeTo(output);
            return output.toByteArray();
        }
    }
}
