<?php
// Migrasi skema untuk fitur revisi: jadwal voting otomatis (voting_start_at/
// voting_end_at di categories) dan tabel activity_logs (audit trail aksi admin).
// Jalankan SATU KALI di server yang database-nya sudah pernah diisi sebelum
// revisi ini ada. Aman dijalankan berkali-kali (idempotent), tidak pernah
// menghapus data.
//
//   CLI    : php migrate_v3.php
//   Browser: http://localhost/<folder-project>/migrate_v3.php
//
// Tidak menghapus dirinya sendiri (sama seperti migrate_v2.php) karena tidak
// menyimpan/mengubah data sensitif -- aman ditinggal untuk verifikasi ulang.

if (PHP_SAPI !== 'cli') {
    $remoteAddr = $_SERVER['REMOTE_ADDR'] ?? '';
    if (!in_array($remoteAddr, ['127.0.0.1', '::1'], true)) {
        http_response_code(403);
        header('Content-Type: text/plain');
        echo "Akses ditolak. migrate_v3.php hanya bisa dijalankan dari localhost.\n";
        exit;
    }
}

require __DIR__ . '/config/database.php';

header('Content-Type: text/plain');

function columnExistsV3(PDO $pdo, string $table, string $column): bool
{
    $stmt = $pdo->prepare(
        'SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?'
    );
    $stmt->execute([$table, $column]);
    return (int) $stmt->fetchColumn() > 0;
}

function tableExistsV3(PDO $pdo, string $table): bool
{
    $stmt = $pdo->prepare(
        'SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?'
    );
    $stmt->execute([$table]);
    return (int) $stmt->fetchColumn() > 0;
}

$applied = [];

if (!columnExistsV3($pdo, 'categories', 'voting_start_at')) {
    $pdo->exec('ALTER TABLE categories ADD COLUMN voting_start_at DATETIME NULL');
    $applied[] = 'categories.voting_start_at ditambahkan';
}

if (!columnExistsV3($pdo, 'categories', 'voting_end_at')) {
    $pdo->exec('ALTER TABLE categories ADD COLUMN voting_end_at DATETIME NULL');
    $applied[] = 'categories.voting_end_at ditambahkan';
}

if (!tableExistsV3($pdo, 'activity_logs')) {
    $pdo->exec(
        'CREATE TABLE activity_logs (
            id INT AUTO_INCREMENT PRIMARY KEY,
            admin_id INT NOT NULL,
            action VARCHAR(100) NOT NULL,
            target_type VARCHAR(50) NOT NULL,
            target_id INT NULL,
            description VARCHAR(255) NOT NULL,
            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_activity_logs_admin FOREIGN KEY (admin_id) REFERENCES users(id)
        ) ENGINE=InnoDB'
    );
    $applied[] = 'tabel activity_logs dibuat';
}

if (empty($applied)) {
    echo "Tidak ada perubahan -- skema sudah up to date.\n";
} else {
    echo "Migrasi selesai:\n";
    foreach ($applied as $line) {
        echo "- {$line}\n";
    }
}
