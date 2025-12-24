package com.example.propertymanager;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatementPdfGenerator {

    public static void generateStatement(Context context, List<PaymentLog> logs, String fromDate, String toDate) {
        PdfDocument document = new PdfDocument();

        // 1. Paints & Styles
        Paint titlePaint = new Paint(); titlePaint.setTextSize(20); titlePaint.setFakeBoldText(true); titlePaint.setColor(Color.BLACK);
        Paint headerPaint = new Paint(); headerPaint.setTextSize(12); headerPaint.setFakeBoldText(true); headerPaint.setColor(Color.DKGRAY);
        Paint textPaint = new Paint(); textPaint.setTextSize(11); textPaint.setColor(Color.BLACK);
        Paint totalPaint = new Paint(); totalPaint.setTextSize(12); totalPaint.setFakeBoldText(true); totalPaint.setColor(Color.parseColor("#006400"));
        Paint linePaint = new Paint(); linePaint.setColor(Color.LTGRAY);

        // 2. Define Columns (X positions)
        int xDate = 40;
        int xRoom = 160;
        int xAmount = 380;
        int xMode = 480;

        int yStart = 140; // Standard start Y for data
        int yCurrent = yStart;
        int yLimit = 780; // Bottom margin limit
        double totalCollected = 0;

        // 3. Start Page 1
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        drawPageBorder(canvas);
        drawWatermark(canvas);
        drawHeaders(canvas, fromDate, toDate, titlePaint, headerPaint, xDate, xRoom, xAmount, xMode);

        // 4. Loop through Logs
        for (PaymentLog log : logs) {

            // --- MULTI-PAGE LOGIC ---
            if (yCurrent > yLimit) {
                document.finishPage(page); // Close old page

                // Create new page
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();

                // Draw Template on new page
                drawPageBorder(canvas);
                drawWatermark(canvas);
                drawHeaders(canvas, fromDate, toDate, titlePaint, headerPaint, xDate, xRoom, xAmount, xMode);

                // RESET Y (Crucial Fix: Set to 140 so it doesn't overlap header line at 120)
                yCurrent = yStart;
            }

            // --- DATA ROWS ---
            // 1. Date
            String niceDate = formatTime(log.getCreatedAt());
            canvas.drawText(niceDate, xDate, yCurrent, textPaint);

            // 2. Room
            String roomInfo = log.getHostelName() +" "+ log.getRoomNumber();
            if (roomInfo.length() > 25) roomInfo = roomInfo.substring(0, 22) + "...";
            canvas.drawText(roomInfo, xRoom, yCurrent, textPaint);

            // 3. Amount
            double amt = log.getAmountPaid();
            totalCollected += amt;
            canvas.drawText("₹ " + (int)amt, xAmount, yCurrent, textPaint);

            // 4. Mode
            String mode = log.getPaymentMode();
            if (mode == null || mode.isEmpty()) mode = "Cash";
            canvas.drawText(mode, xMode, yCurrent, textPaint);

            // Row Separator
            canvas.drawLine(40, yCurrent + 10, 555, yCurrent + 10, linePaint);
            yCurrent += 30; // Move down for next row
        }

        // --- TOTALS SECTION ---
        // Check if there is space for totals, else create new page
        if (yCurrent > yLimit - 50) {
            document.finishPage(page);
            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            drawPageBorder(canvas);
            yCurrent = 60; // Start at top if it's just totals
        }

        // Draw Thick Line above Total
        Paint boldLine = new Paint(); boldLine.setStrokeWidth(2f);
        canvas.drawLine(40, yCurrent, 555, yCurrent, boldLine);
        yCurrent += 30;

        canvas.drawText("TOTAL COLLECTED:", xRoom, yCurrent, totalPaint);
        canvas.drawText("₹ " + (int)totalCollected, xAmount, yCurrent, totalPaint);

        document.finishPage(page);

        // 5. Save & Share
        String fileName = "Statement_" + System.currentTimeMillis() + ".pdf";
        File file = new File(context.getExternalCacheDir(), fileName);

        try {
            document.writeTo(new FileOutputStream(file));
            document.close();
            sharePdf(context, file);
        } catch (Exception e) {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- HELPERS ---

    private static void drawHeaders(Canvas canvas, String from, String to, Paint tP, Paint hP, int xD, int xR, int xA, int xM) {
        // Title
        canvas.drawText("PAYMENT STATEMENT", 40, 50, tP);
        // Subtitle
        canvas.drawText("From: " + from + "   To: " + to, 40, 75, hP);

        // Table Headers
        canvas.drawText("Date / Time", xD, 110, hP);
        canvas.drawText("Room No.", xR, 110, hP);
        canvas.drawText("Amount", xA, 110, hP);
        canvas.drawText("Mode", xM, 110, hP);

        // Header Line (at Y=120)
        canvas.drawLine(40, 120, 555, 120, hP);
    }

    private static void drawPageBorder(Canvas canvas) {
        Paint p = new Paint(); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
        canvas.drawRect(20, 20, 575, 822, p);
    }

    private static void drawWatermark(Canvas canvas) {
        Paint p = new Paint(); p.setColor(Color.LTGRAY); p.setAlpha(40); p.setTextSize(100);
        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.CENTER);
        canvas.save();
        canvas.rotate(-45, 297, 421);
        canvas.drawText("CONFIDENTIAL", 297, 421, p);
        canvas.restore();
    }

    private static String formatTime(String iso) {
        if (iso == null) return "-";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = in.parse(iso);
            SimpleDateFormat out = new SimpleDateFormat("dd-MMM HH:mm", Locale.US);
            return out.format(date);
        } catch (Exception e) { return iso; }
    }

    private static void sharePdf(Context context, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share Statement"));
        } catch (Exception e) {
            Toast.makeText(context, "Share Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}