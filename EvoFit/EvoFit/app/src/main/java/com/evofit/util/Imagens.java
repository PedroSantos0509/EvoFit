package com.evofit.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;

import com.evofit.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class Imagens {
    private Imagens() {}

    /** Copia a imagem escolhida na galeria para a pasta interna do app (reduzida) e devolve o caminho. */
    public static String salvarFotoPerfil(Context c, Uri uri, long idUsuario) {
        try {
            Bitmap bmp;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(in, null, o);
                int amostra = 1;
                while (o.outWidth / amostra > 800) amostra *= 2;
                try (InputStream in2 = c.getContentResolver().openInputStream(uri)) {
                    BitmapFactory.Options o2 = new BitmapFactory.Options();
                    o2.inSampleSize = amostra;
                    bmp = BitmapFactory.decodeStream(in2, null, o2);
                }
            }
            if (bmp == null) return null;
            File f = new File(c.getFilesDir(), "foto_" + idUsuario + "_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            }
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /** Mostra a foto do caminho; se não houver, mostra o ícone padrão. */
    public static void carregar(ImageView iv, String caminho) {
        if (caminho != null && !caminho.isEmpty() && new File(caminho).exists()) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;
            Bitmap bmp = BitmapFactory.decodeFile(caminho, o);
            if (bmp != null) {
                iv.setPadding(0, 0, 0, 0);
                iv.setImageBitmap(bmp);
                return;
            }
        }
        int pad = (int) (12 * iv.getResources().getDisplayMetrics().density);
        iv.setPadding(pad, pad, pad, pad);
        iv.setImageResource(R.drawable.ic_person);
    }
}
