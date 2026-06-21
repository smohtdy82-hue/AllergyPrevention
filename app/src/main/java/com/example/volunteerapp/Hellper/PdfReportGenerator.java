package com.example.volunteerapp.Hellper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.example.volunteerapp.model.Organization;
import com.example.volunteerapp.model.Student;
import com.example.volunteerapp.model.VolunteerHour;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * مولّد تقارير PDF لساعات التطوع.
 * ينشئ مستند PDF بتنسيق RTL يحتوي على بيانات الطالب والمؤسسة
 * وجدول تفصيلي بالساعات مع دعم التقسيم التلقائي للصفحات.
 */
public class PdfReportGenerator {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final float MARGIN = 40f;

    /**
     * إنشاء ملف PDF يحتوي على تقرير ساعات التطوع للطالب في مؤسسة معيّنة.
     * يتضمن التقرير رأس الصفحة وبيانات الطالب وجدول الساعات والمجموع.
     *
     * @param context  سياق التطبيق للوصول إلى مجلد الكاش
     * @param student  بيانات الطالب (الاسم، رقم الهوية)
     * @param org      بيانات المؤسسة
     * @param hours    قائمة سجلات ساعات التطوع
     * @return ملف PDF المُنشأ في مجلد الكاش
     * @throws IOException في حال فشل كتابة الملف
     */
    public static File generateReport(Context context, Student student, Organization org, List<VolunteerHour> hours) throws IOException {
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint titlePaint = new Paint();
        titlePaint.setTextSize(20f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        Paint headerPaint = new Paint();
        headerPaint.setTextSize(14f);
        headerPaint.setFakeBoldText(true);

        Paint bodyPaint = new Paint();
        bodyPaint.setTextSize(12f);

        Paint linePaint = new Paint();
        linePaint.setColor(0xFFCCCCCC);
        linePaint.setStrokeWidth(1f);

        float y = MARGIN + 30f;

        canvas.drawText("تقرير ساعات التطوع", PAGE_WIDTH / 2f, y, titlePaint);
        y += 40f;

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
        y += 25f;

        String studentName = student.getName() != null ? student.getName() : "";
        String studentId = student.getIdNumber() != null ? student.getIdNumber() : "";
        String orgName = org.getName() != null ? org.getName() : "";

        drawRtlText(canvas, "اسم الطالب: " + studentName, PAGE_WIDTH - MARGIN, y, headerPaint);
        y += 22f;
        drawRtlText(canvas, "رقم الهوية: " + studentId, PAGE_WIDTH - MARGIN, y, bodyPaint);
        y += 22f;
        drawRtlText(canvas, "المؤسسة: " + orgName, PAGE_WIDTH - MARGIN, y, bodyPaint);
        y += 30f;

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
        y += 25f;

        float colDate = PAGE_WIDTH - MARGIN;
        float colDesc = PAGE_WIDTH - MARGIN - 160f;
        float colHours = MARGIN + 60f;

        drawRtlText(canvas, "التاريخ", colDate, y, headerPaint);
        drawRtlText(canvas, "الوصف", colDesc, y, headerPaint);
        drawRtlText(canvas, "الساعات", colHours, y, headerPaint);
        y += 5f;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
        y += 18f;

        int totalHours = 0;
        for (VolunteerHour h : hours) {
            if (y > PAGE_HEIGHT - MARGIN - 60f) {
                doc.finishPage(page);
                PdfDocument.PageInfo nextInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, doc.getPages().size() + 1).create();
                page = doc.startPage(nextInfo);
                canvas = page.getCanvas();
                y = MARGIN + 30f;
            }

            String date = h.getDate() != null ? h.getDate() : "";
            if (date.length() > 20) date = date.substring(0, 20);
            String desc = h.getDescription() != null ? h.getDescription() : "";
            if (desc.length() > 30) desc = desc.substring(0, 30);

            drawRtlText(canvas, date, colDate, y, bodyPaint);
            drawRtlText(canvas, desc, colDesc, y, bodyPaint);
            drawRtlText(canvas, String.valueOf(h.getHours()), colHours, y, bodyPaint);
            totalHours += h.getHours();
            y += 20f;
        }

        y += 10f;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
        y += 25f;

        drawRtlText(canvas, "المجموع: " + totalHours + " ساعة", PAGE_WIDTH - MARGIN, y, headerPaint);

        doc.finishPage(page);

        File dir = new File(context.getCacheDir(), "reports");
        if (!dir.exists()) dir.mkdirs();
        String fileName = "تقرير_" + studentName.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
        File file = new File(dir, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        doc.writeTo(fos);
        fos.close();
        doc.close();

        return file;
    }

    /** رسم نص بمحاذاة من اليمين إلى اليسار (RTL) على لوحة الرسم. */
    private static void drawRtlText(Canvas canvas, String text, float x, float y, Paint paint) {
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(text, x, y, paint);
    }

    /**
     * إنشاء Intent لمشاركة ملف PDF عبر التطبيقات الأخرى (واتساب، بريد، إلخ).
     *
     * @param context سياق التطبيق
     * @param pdfFile ملف PDF المراد مشاركته
     * @return Intent جاهز للإطلاق مع نافذة اختيار التطبيق
     */
    public static Intent createShareIntent(Context context, File pdfFile) {
        Uri uri = FileProvider.getUriForFile(context, "com.example.volunteerapp.fileprovider", pdfFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, "تقرير ساعات التطوع");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(intent, "مشاركة التقرير");
    }
}
