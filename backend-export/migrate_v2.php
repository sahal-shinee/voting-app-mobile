<?php
// Migrasi skema untuk fitur revisi: soft-delete kategori (riwayat/restore/hapus
// permanen) dan soft-delete siswa. Jalankan SATU KALI di server yang database-nya
// sudah pernah diisi sebelum revisi ini ada. Aman dijalankan berkali-kali --
// hanya menambah kolom yang belum ada (idempotent), tidak pernah menghapus data.
//
//   CLI    : php migrate_v2.php
//   Browser: http://localhost/<folder-project>/migrate_v2.php
//
// Tidak menghapus dirinya sendiri (beda dari seed.php/reset_admin.php) karena
// tidak menyimpan/mengubah data sensitif -- aman ditinggal untuk verifikasi ulang.

if (PHP_SAPI !== 'cli') {
    $remoteAddr = $_SERVER['REMOTE_ADDR'] ?? '';
    if (!in_array($remoteAddr, ['127.0.0.1', '::1'], true)) {
        http_response_code(403);
        header('Content-Type: text/plain');
        echo "Akses ditolak. migrate_v2.php hanya bisa dijalankan dari localhost.\n";
        exit;
    }
}

require __DIR__ . '/config/database.php';

header('Content-Type: text/plain');

function columnExists(PDO $pdo, string $table, string $column): bool
{
    $stmt = $pdo->prepare(
        'SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?'
    );
    $stmt->execute([$table, $column]);
    return (int) $stmt->fetchColumn() > 0;
}

$applied = [];

if (!columnExists($pdo, 'categories', 'deleted_at')) {
    $pdo->exec('ALTER TABLE categories ADD COLUMN deleted_at DATETIME NULL');
    $applied[] = 'categories.deleted_at ditambahkan';
}

if (!columnExists($pdo, 'users', 'deleted_at')) {
    $pdo->exec('ALTER TABLE users ADD COLUMN deleted_at DATETIME NULL');
    $applied[] = 'users.deleted_at ditambahkan';
}

if (empty($applied)) {
    echo "Tidak ada perubahan -- skema sudah up to date.\n";
} else {
    echo "Migrasi selesai:\n";
    foreach ($applied as $line) {
        echo "- {$line}\n";
    }
}
