package com.cwpdf.saver;

import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cwpdf.saver.db.PdfDatabaseHelper;
import com.cwpdf.saver.util.DownloadEngine;
import com.google.android.material.button.MaterialButton;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PdfViewHolder> {

    private Cursor cursor;

    public PdfAdapter(Cursor cursor) {
        this.cursor = cursor;
    }

    public void setCursor(Cursor newCursor) {
        if (cursor != null) {
            cursor.close();
        }
        cursor = newCursor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) {
            return;
        }

        String title = cursor.getString(cursor.getColumnIndexOrThrow(PdfDatabaseHelper.COLUMN_TITLE));
        String url = cursor.getString(cursor.getColumnIndexOrThrow(PdfDatabaseHelper.COLUMN_URL));
        String uriString = cursor.getString(cursor.getColumnIndexOrThrow(PdfDatabaseHelper.COLUMN_URI));
        String key = cursor.getString(cursor.getColumnIndexOrThrow(PdfDatabaseHelper.COLUMN_KEY));
        boolean isEncrypted = cursor.getInt(cursor.getColumnIndexOrThrow(PdfDatabaseHelper.COLUMN_IS_ENCRYPTED)) == 1;

        holder.textTitle.setText(title != null && !title.isEmpty() ? title : "Unknown PDF");
        holder.textStatus.setText(isEncrypted ? "🔒 Encrypted PDF" : "📄 Standard PDF");

        holder.btnDownload.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Starting download...", Toast.LENGTH_SHORT).show();
            holder.btnDownload.setEnabled(false);
            holder.btnDownload.setText("Downloading...");

            Uri localUri = (uriString != null && !uriString.isEmpty()) ? Uri.parse(uriString) : null;

            DownloadEngine.startDownload(v.getContext(), url, localUri, key, title, isEncrypted, new DownloadEngine.DownloadCallback() {
                @Override
                public void onSuccess(String fileName) {
                    holder.itemView.post(() -> {
                        Toast.makeText(v.getContext(), "Saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                        holder.btnDownload.setEnabled(true);
                        holder.btnDownload.setText("Download");
                    });
                }

                @Override
                public void onError(String errorMsg) {
                    holder.itemView.post(() -> {
                        Toast.makeText(v.getContext(), errorMsg, Toast.LENGTH_LONG).show();
                        holder.btnDownload.setEnabled(true);
                        holder.btnDownload.setText("Retry");
                    });
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textStatus;
        MaterialButton btnDownload;

        PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            textStatus = itemView.findViewById(R.id.text_status);
            btnDownload = itemView.findViewById(R.id.btn_download);
        }
    }
}
