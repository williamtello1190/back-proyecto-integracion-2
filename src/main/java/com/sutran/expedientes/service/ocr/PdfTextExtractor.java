package com.sutran.expedientes.service.ocr;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Extrae el texto plano de un PDF adjunto usando su capa de texto (PDFBox).
 * Los expedientes de SUTRAN se generan digitalmente (no son escaneos), por lo
 * que el texto ya está embebido en el PDF y no se requiere OCR (Tesseract).
 */
@Component
public class PdfTextExtractor {

    public String extraerTexto(Path rutaPdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(rutaPdf.toFile())) {
            PDFTextStripper extractor = new PDFTextStripper();
            return extractor.getText(documento);
        }
    }
}
