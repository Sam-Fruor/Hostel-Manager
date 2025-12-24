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
import java.util.List;

public class PdfGenerator {

    public static void generateReport(Context context, List<RoomData> roomList, String month, String year, String propertyName) {
        PdfDocument document = new PdfDocument();

        // 1. Paints
        Paint titlePaint = new Paint(); titlePaint.setTextSize(20); titlePaint.setFakeBoldText(true);
        Paint headerPaint = new Paint(); headerPaint.setTextSize(11); headerPaint.setFakeBoldText(true); headerPaint.setColor(Color.DKGRAY);
        Paint textPaint = new Paint(); textPaint.setTextSize(10); textPaint.setColor(Color.BLACK);
        Paint totalPaint = new Paint(); totalPaint.setTextSize(11); totalPaint.setFakeBoldText(true); totalPaint.setColor(Color.parseColor("#006400"));
        Paint linePaint = new Paint(); linePaint.setColor(Color.LTGRAY);

        // 2. Columns
        int xRoom = 30, xName = 80, xRent = 240, xLastDue = 320, xTotalDue = 410, xPaid = 500;
        int yCurrent = 140;
        int yLimit = 780;

        // Totals
        double sumRent = 0, sumLastDue = 0, sumTotalDue = 0, sumPaid = 0;

        // 3. Start First Page
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // --- DRAW BORDER & WATERMARK (Page 1) ---
        drawPageBorder(canvas);
        drawWatermark(canvas);

        drawHeaders(canvas, propertyName, month, year, titlePaint, headerPaint, xRoom, xName, xRent, xLastDue, xTotalDue, xPaid);

        for (RoomData room : roomList) {
            if (room.isVacant()) continue;

            if (yCurrent > yLimit) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();

                // --- DRAW BORDER & WATERMARK (New Pages) ---
                drawPageBorder(canvas);
                drawWatermark(canvas);

                yCurrent = 80;
                drawHeaders(canvas, propertyName, month, year, titlePaint, headerPaint, xRoom, xName, xRent, xLastDue, xTotalDue, xPaid);
                yCurrent = 110;
            }

            // Calculations
            double rent = room.getStandardRent();
            double totalDue = room.getCurrentDue();
            double expected = room.getExpectedAmount();
            if (expected == 0) expected = Math.max(totalDue, rent);
            double lastDue = Math.max(0, expected - rent);
            double paid = Math.max(0, expected - totalDue);

            sumRent += rent; sumLastDue += lastDue; sumTotalDue += totalDue; sumPaid += paid;

            // Draw Row
            canvas.drawText(room.getRoomNumber(), xRoom, yCurrent, textPaint);
            String name = room.getTenantName();
            if (name.length() > 22) name = name.substring(0, 20) + "...";
            canvas.drawText(name, xName, yCurrent, textPaint);
            canvas.drawText("₹" + (int)rent, xRent, yCurrent, textPaint);
            canvas.drawText("₹" + (int)lastDue, xLastDue, yCurrent, textPaint);

            Paint dueTextPaint = new Paint(textPaint);
            if (totalDue > 0) dueTextPaint.setColor(Color.RED);
            canvas.drawText("₹" + (int)totalDue, xTotalDue, yCurrent, dueTextPaint);
            canvas.drawText("₹" + (int)paid, xPaid, yCurrent, textPaint);

            canvas.drawLine(30, yCurrent + 8, 560, yCurrent + 8, linePaint);
            yCurrent += 30;
        }

        // Totals
        if (yCurrent > yLimit - 60) {
            document.finishPage(page);
            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            drawPageBorder(canvas); // Border for totals page
            drawWatermark(canvas);
            yCurrent = 60;
        }

        Paint boldLine = new Paint(); boldLine.setStrokeWidth(2f);
        canvas.drawLine(30, yCurrent, 560, yCurrent, boldLine);
        yCurrent += 25;

        canvas.drawText("TOTALS:", xName, yCurrent, totalPaint);
        canvas.drawText("₹" + (int)sumRent, xRent, yCurrent, totalPaint);
        canvas.drawText("₹" + (int)sumLastDue, xLastDue, yCurrent, totalPaint);
        Paint totalDuePaint = new Paint(totalPaint);
        if (sumTotalDue > 0) totalDuePaint.setColor(Color.RED);
        canvas.drawText("₹" + (int)sumTotalDue, xTotalDue, yCurrent, totalDuePaint);
        canvas.drawText("₹" + (int)sumPaid, xPaid, yCurrent, totalPaint);

        document.finishPage(page);

        // Save & Share
        String fileName = propertyName + "_Report_" + month + "_" + System.currentTimeMillis() + ".pdf";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        try {
            document.writeTo(new FileOutputStream(file));
            document.close();
            sharePdf(context, file);
        } catch (Exception e) {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- NEW METHOD FOR BORDER ---
    private static void drawPageBorder(Canvas canvas) {
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        // Draw rectangle around the page (A4 size: 595 x 842)
        // We leave a margin of 20 pixels on all sides
        canvas.drawRect(20, 20, 575, 822, borderPaint);
    }

    private static void drawWatermark(Canvas canvas) {
        Paint watermarkPaint = new Paint();
        watermarkPaint.setColor(Color.LTGRAY);
        watermarkPaint.setAlpha(40);
        watermarkPaint.setTextSize(100);
        watermarkPaint.setTypeface(Typeface.DEFAULT_BOLD);
        watermarkPaint.setTextAlign(Paint.Align.CENTER);
        canvas.save();
        canvas.rotate(-45, 595 / 2, 842 / 2);
        canvas.drawText("CONFIDENTIAL", 595 / 2, 842 / 2, watermarkPaint);
        canvas.restore();
    }

    private static void sharePdf(Context context, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share Report via..."));
        } catch (Exception e) {
            Toast.makeText(context, "Share Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void drawHeaders(Canvas canvas, String prop, String m, String y, Paint tP, Paint hP, int xR, int xN, int xRent, int xLast, int xTotal, int xPaid) {
        canvas.drawText(prop + " Rent Report", 30, 50, tP);
        canvas.drawText("Month: " + m + " " + y, 30, 75, hP);
        canvas.drawText("Room", xR, 110, hP);
        canvas.drawText("Tenant Name", xN, 110, hP);
        canvas.drawText("Rent", xRent, 110, hP);
        canvas.drawText("Old Due", xLast, 110, hP);
        canvas.drawText("Total Due", xTotal, 110, hP);
        canvas.drawText("Paid", xPaid, 110, hP);
        canvas.drawLine(30, 115, 560, 115, hP);
    }
}