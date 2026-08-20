<?php

class Upload
{
    private const MAX_BYTES = 2 * 1024 * 1024; // 2MB
    private const ALLOWED_MIME = [
        'image/jpeg' => 'jpg',
        'image/png' => 'png',
    ];

    public static function saveCandidatePhoto(array $file): string
    {
        if (!isset($file['error']) || $file['error'] !== UPLOAD_ERR_OK) {
            Response::error('Upload foto gagal', 422);
        }

        if ($file['size'] > self::MAX_BYTES) {
            Response::error('Ukuran foto maksimal 2MB', 422);
        }

        // Deteksi MIME dari isi file yang sebenarnya, bukan dari nama file atau
        // Content-Type kiriman klien -- ini yang mencegah trik ekstensi ganda
        // (mis. "foto.jpg.php") atau file non-gambar yang diberi nama .jpg.
        $finfo = new finfo(FILEINFO_MIME_TYPE);
        $mime = $finfo->file($file['tmp_name']);

        if (!isset(self::ALLOWED_MIME[$mime])) {
            Response::error('Format foto harus JPEG atau PNG', 422);
        }

        $ext = self::ALLOWED_MIME[$mime];
        $filename = bin2hex(random_bytes(16)) . '.' . $ext;
        $destPath = __DIR__ . '/../uploads/candidates/' . $filename;

        // Request lewat MultipartParser (PUT) menghasilkan file sementara biasa,
        // bukan upload HTTP asli -- is_uploaded_file() akan false untuk itu, jadi
        // perlu fallback ke rename().
        $moved = is_uploaded_file($file['tmp_name'])
            ? move_uploaded_file($file['tmp_name'], $destPath)
            : rename($file['tmp_name'], $destPath);

        if (!$moved) {
            Response::error('Gagal menyimpan foto', 500);
        }

        return 'uploads/candidates/' . $filename;
    }

    public static function deleteCandidatePhoto(?string $relativePath): void
    {
        if (!$relativePath) {
            return;
        }

        $path = __DIR__ . '/../' . $relativePath;
        if (is_file($path)) {
            @unlink($path);
        }
    }
}
